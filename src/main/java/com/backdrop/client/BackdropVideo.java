package com.backdrop.client;

import com.backdrop.BackdropConfig;
import mchorse.bbs_mod.graphics.texture.Texture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A video that plays by itself in a loop, with its sound, for the banner.
 *
 * <p><b>The sound is the clock.</b> Two ffmpeg processes decode the same file, one for the picture and
 * one for the sound, and neither is paced by ffmpeg ({@code -re} jitters, and it let the two drift
 * apart). The sound goes to the sound device; how much of it the device has actually played is the
 * time. The picture is decoded ahead into a small queue, and the render thread shows the newest frame
 * whose time has come, dropping the ones that are late. A picture that can't keep up skips frames
 * instead of falling behind the sound, and one that is ahead waits. With no sound (volume 0, or a video
 * without any) the wall clock stands in.</p>
 *
 * <p>The sync offset moves only which frame is picked, so it can be changed live.</p>
 */
public class BackdropVideo
{
    private static final Map<String, BackdropVideo> VIDEOS = new HashMap<>();

    private static final Pattern SIZE_PATTERN = Pattern.compile("Video:.*?(\\d{2,5})x(\\d{2,5})");
    private static final Pattern DURATION_PATTERN = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+(?:\\.\\d+)?)");
    private static final Pattern FPS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?) fps");

    private static final int[] QUALITY_WIDTH = {480, 854, 1280};
    private static final double[] QUALITY_FPS = {20, 30, 60};
    private static final double MAX_FPS = 60;
    private static final double DEFAULT_FPS = 30;

    private static final int SAMPLE_RATE = 44100;
    private static final int FRAME_BYTES = 4;

    /* Frames decoded ahead of the clock: enough to ride out a hiccup, few enough to stay lean */
    private static final int QUEUE = 6;

    private static final double LAG_LIMIT = 0.6;
    private static final long IDLE_NANOS = 3_000_000_000L;
    private static final long SOUND_HOLD_NANOS = 1_000_000_000L;
    private static final long RETRY_NANOS = 5_000_000_000L;
    private static final long AUDIO_WAIT_NANOS = 2_000_000_000L;

    private final File file;
    private final String url;

    private Texture texture;
    private Session session;
    private volatile long lastUsed;

    /* When the video first started, and how long it is: a session begun later seeks to where it would be by now, so leaving the page and coming back does not start it over */
    private volatile long epoch;
    private volatile double duration;
    private volatile int[] probedSize;
    private volatile double probedFps;
    private volatile long failedAt;
    private volatile String error;
    private long soundUntil;
    private float soundVolume = 1F;

    public static BackdropVideo get(String url, File file)
    {
        String key = url != null ? url : file.getAbsolutePath();

        synchronized (VIDEOS)
        {
            return VIDEOS.computeIfAbsent(key, (k) -> new BackdropVideo(url == null ? file : null, url));
        }
    }

    private BackdropVideo(File file, String url)
    {
        this.file = file;
        this.url = url;
    }

    public String getError()
    {
        if (this.file != null && !this.file.isFile())
        {
            return "Không tìm thấy file video: " + this.file.getName();
        }

        return this.error;
    }

    /** Render thread only: the frame to show now, or null while there is none. */
    public Texture getFrame(float volume, int offsetMs)
    {
        long now = System.nanoTime();

        this.lastUsed = now;

        if (this.file != null && !this.file.isFile())
        {
            return null;
        }

        /* The sound is not played from here: BackdropAudio plays it on its own, so a picture that
         * stutters does not drag it along, and it goes on when the banner is not on screen */
        boolean wantSound = false;
        String key = this.url != null ? this.url : this.file.getAbsolutePath();
        boolean synced = BackdropAudio.position(key) >= 0;
        Session current = this.session;

        /* The sound switched on or off, or the session ended: start a fresh one */
        if (current != null && (current.sound != wantSound || current.finished || current.synced != synced || current.quality != BackdropConfig.get().quality || (synced && current.audioGeneration != BackdropAudio.generation())))
        {
            current.shutdown();
            current = null;
            this.session = null;
        }

        if (current == null && (this.error == null || now - this.failedAt > RETRY_NANOS))
        {
            this.error = null;
            current = new Session(wantSound);
            this.session = current;
            current.start();
        }

        if (current == null)
        {
            return this.texture;
        }

        current.volume = this.soundVolume;

        double target = current.clock() + offsetMs / 1000.0;
        Frame picked = null;

        while (true)
        {
            Frame head = current.frames.peek();

            if (head == null || head.time > target)
            {
                break;
            }

            Frame next = current.frames.poll();

            if (picked != null)
            {
                current.free.offer(picked.data);
            }

            picked = next;
        }

        if (picked != null)
        {
            this.upload(picked.data, current.width, current.height);
            current.free.offer(picked.data);

            current.shown = picked.time;
            current.hasShown = true;
        }

        /* Following the sound, a picture that cannot keep up (the machine hitched) does not run
         * fast to catch up: after a moment behind, it starts over at where the sound is now */
        if (current.synced)
        {
            double lag = target - current.shown;

            if (!current.hasShown ? now - current.startedAt > 2_500_000_000L : lag > LAG_LIMIT)
            {
                if (current.lagSince == 0)
                {
                    current.lagSince = now;
                }
                else if (now - current.lagSince > 400_000_000L && now - current.startedAt > 1_500_000_000L)
                {
                    current.shutdown();
                    this.session = null;
                }
            }
            else
            {
                current.lagSince = 0;
            }
        }

        return this.texture;
    }

    private void upload(ByteBuffer data, int width, int height)
    {
        if (this.texture == null)
        {
            this.texture = new Texture();

            this.texture.setFilter(GL11.GL_LINEAR);
            this.texture.setWrap(GL13.GL_CLAMP_TO_EDGE);
        }

        this.texture.bind();
        this.texture.uploadTexture(GL11.GL_TEXTURE_2D, 0, width, height, data);
        this.texture.unbind();
    }

    private static class Frame
    {
        public final ByteBuffer data;
        public final double time;

        public Frame(ByteBuffer data, double time)
        {
            this.data = data;
            this.time = time;
        }
    }

    /** One run of the decoders: dies when the video isn't asked for any more, or when something breaks. */
    private class Session
    {
        public final boolean sound;

        public final BlockingQueue<Frame> frames = new ArrayBlockingQueue<>(QUEUE);
        public final BlockingQueue<ByteBuffer> free = new ArrayBlockingQueue<>(QUEUE + 3);

        public volatile int width;
        public volatile int height;
        public volatile boolean finished;
        public volatile float volume;

        public final int quality = BackdropConfig.get().quality;
        public volatile double shown;
        public volatile boolean hasShown;
        public long lagSince;
        public final long startedAt = System.nanoTime();

        private volatile boolean stop;
        private volatile Process videoProcess;
        private volatile Process audioProcess;
        private volatile SourceDataLine line;

        /* The sound's clock: frames handed to the device, minus those still waiting in it */
        private volatile long written;
        private volatile boolean audioDead;
        private volatile long audioStarted;

        /* The wall clock, for when there is no sound */
        private long wallStart = -1;
        private double lastClock;
        private double deadBase = -1;
        private long deadAt;

        /* Following the sound that plays on its own: where it was when this run began, and which run of it that was */
        public final boolean synced;
        public volatile double audioBase;
        public final int audioGeneration;
        private final String audioKey;

        public Session(boolean sound)
        {
            this.sound = sound;
            this.volume = 1F;
            this.audioKey = BackdropVideo.this.url != null ? BackdropVideo.this.url : BackdropVideo.this.file.getAbsolutePath();
            this.audioGeneration = BackdropAudio.generation();

            double base = BackdropAudio.position(this.audioKey);

            this.synced = base >= 0;
            this.audioBase = base;
        }

        public void start()
        {
            Thread thread = new Thread(this::runVideo, "BBS Banner Video");

            thread.setDaemon(true);
            thread.start();
        }

        public void shutdown()
        {
            this.stop = true;

            Process video = this.videoProcess;
            Process audio = this.audioProcess;
            SourceDataLine device = this.line;

            if (video != null)
            {
                video.destroy();
            }

            if (audio != null)
            {
                audio.destroy();
            }

            if (device != null)
            {
                device.close();
            }
        }

        private boolean smoothed;
        private double smooth;
        private double lastRaw;
        private long lastNow;
        private long lastChange;

        /**
         * The sound device reports its position in coarse steps, and a picture that follows the steps
         * moves like stop-motion. Between reports the clock runs on by itself and is only eased toward
         * the reported position; when the sound stands still (paused) it stands still too.
         */
        private double smooth(long now, double raw)
        {
            if (!this.smoothed)
            {
                this.smoothed = true;
                this.smooth = raw;
                this.lastRaw = raw;
                this.lastNow = now;
                this.lastChange = now;

                return raw;
            }

            double dt = (now - this.lastNow) / 1e9;

            this.lastNow = now;

            if (raw != this.lastRaw)
            {
                this.lastRaw = raw;
                this.lastChange = now;
            }

            if (now - this.lastChange > 400_000_000L)
            {
                this.smooth = raw;
            }
            else
            {
                this.smooth += dt;

                double error = raw - this.smooth;

                if (Math.abs(error) > 0.25)
                {
                    this.smooth = raw;
                }
                else
                {
                    this.smooth += error * Math.min(1D, dt * 4);
                }
            }

            return this.smooth;
        }

        /** The time, in seconds, the picture should be at: the sound's position while it plays, else the wall clock. */
        public double clock()
        {
            double time;
            long now = System.nanoTime();

            /* The picture follows the sound: it is where the sound has got to since this run began, so a stutter in one catches the other up */
            if (this.synced)
            {
                double position = BackdropAudio.position(this.audioKey);

                if (position >= 0)
                {
                    this.lastClock = this.smooth(now, Math.max(0, position - this.audioBase));

                    return this.lastClock;
                }
            }

            boolean useSound = this.sound && !this.audioDead && this.line != null && this.written > 0;

            if (useSound)
            {
                SourceDataLine device = this.line;
                long queued = (device.getBufferSize() - device.available()) / FRAME_BYTES;
                long played = Math.max(0L, this.written / FRAME_BYTES - queued);

                time = played / (double) SAMPLE_RATE;
            }
            else if (this.sound && !this.audioDead && now - this.audioStarted < AUDIO_WAIT_NANOS)
            {
                /* The sound hasn't produced anything yet: the first frame waits for it */
                time = 0;
            }
            else
            {
                /* No sound, or it died: the wall clock, carrying on from where the sound left the picture */
                if (this.sound && this.deadBase < 0)
                {
                    this.deadBase = this.lastClock;
                    this.deadAt = now;
                }

                if (this.sound)
                {
                    time = this.deadBase + (now - this.deadAt) / 1e9;
                }
                else
                {
                    if (this.wallStart < 0)
                    {
                        this.wallStart = now;
                    }

                    time = (now - this.wallStart) / 1e9;
                }
            }

            this.lastClock = time;

            return time;
        }

        private void runVideo()
        {
            try
            {
                String ffmpeg = Ffmpeg.path();
                String input = this.resolveInput();

                double fps = DEFAULT_FPS;
                /* What the video is does not change between runs: ask ffmpeg once, so a restart is quick */
                int[] size = BackdropVideo.this.probedSize;

                if (size == null)
                {
                    size = this.probe(ffmpeg, input);

                    if (size == null)
                    {
                        return;
                    }

                    BackdropVideo.this.probedFps = this.fps;
                    BackdropVideo.this.probedSize = size;
                }
                else
                {
                    this.fps = BackdropVideo.this.probedFps;
                }

                /* Where the sound is now, after the time spent getting here */
                if (this.synced)
                {
                    double now = BackdropAudio.position(this.audioKey);

                    if (now >= 0)
                    {
                        this.audioBase = now;
                    }
                }

                long begun = System.nanoTime();

                if (BackdropVideo.this.epoch == 0)
                {
                    BackdropVideo.this.epoch = begun;
                }

                double length = BackdropVideo.this.duration;
                double elapsed = this.synced ? this.audioBase : (begun - BackdropVideo.this.epoch) / 1e9;
                double resume = length > 1 ? elapsed % length : 0;

                int cap = Math.max(0, Math.min(2, this.quality));

                fps = Math.min(this.fps, QUALITY_FPS[cap]);

                int w = Math.max(2, Math.min(size[0], QUALITY_WIDTH[cap]) & ~1);
                int h = Math.max(2, (int) Math.round(size[1] * (w / (double) size[0])) & ~1);

                this.width = w;
                this.height = h;

                for (int i = 0; i < QUEUE + 3; i++)
                {
                    this.free.offer(ByteBuffer.allocateDirect(w * h * 4));
                }

                ProcessBuilder builder = new ProcessBuilder(
                    ffmpeg, "-hide_banner", "-loglevel", "quiet", "-nostdin",
                    "-stream_loop", "-1", "-ss", String.format(java.util.Locale.ROOT, "%.3f", resume), "-i", input,
                    "-an", "-sn", "-dn",
                    "-vf", "fps=" + fps + ",scale=" + w + ":" + h + ":flags=fast_bilinear",
                    "-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1"
                );

                builder.redirectError(ProcessBuilder.Redirect.DISCARD);

                this.audioStarted = System.nanoTime();

                /* The sound first: its clock is the one the picture follows */
                if (this.sound)
                {
                    this.startAudio(ffmpeg, input);
                }

                Process process = builder.start();

                this.videoProcess = process;

                ReadableByteChannel channel = Channels.newChannel(process.getInputStream());
                double step = 1.0 / fps;
                long count = 0;

                while (!this.stop)
                {
                    if (System.nanoTime() - BackdropVideo.this.lastUsed > IDLE_NANOS)
                    {
                        break;
                    }

                    ByteBuffer buffer = this.free.poll(100, TimeUnit.MILLISECONDS);

                    if (buffer == null)
                    {
                        continue;
                    }

                    buffer.clear();

                    while (buffer.hasRemaining())
                    {
                        if (channel.read(buffer) < 0)
                        {
                            if (count == 0)
                            {
                                BackdropVideo.this.error = "ffmpeg không giải mã được video này (thử file .mp4 H.264)";
                            }

                            return;
                        }
                    }

                    buffer.flip();

                    Frame frame = new Frame(buffer, count * step);

                    count++;

                    /* Blocks while the queue is full: the pipe backs up and ffmpeg waits, so decoding never runs ahead */
                    while (!this.stop && !this.frames.offer(frame, 100, TimeUnit.MILLISECONDS))
                    {
                        if (System.nanoTime() - BackdropVideo.this.lastUsed > IDLE_NANOS)
                        {
                            return;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                if (!this.stop)
                {
                    BackdropVideo.this.error = "Không chạy được ffmpeg (đặt đường dẫn ffmpeg trong Settings > Video): " + e.getMessage();
                }
            }
            finally
            {
                this.shutdown();

                if (BackdropVideo.this.error != null)
                {
                    BackdropVideo.this.failedAt = System.nanoTime();
                }

                this.finished = true;
            }
        }

        private void startAudio(String ffmpeg, String input) throws Exception
        {
            ProcessBuilder builder = new ProcessBuilder(
                ffmpeg, "-hide_banner", "-loglevel", "quiet", "-nostdin",
                "-stream_loop", "-1", "-i", input,
                "-vn", "-sn", "-dn", "-map", "0:a:0?",
                "-ac", "2", "-ar", String.valueOf(SAMPLE_RATE), "-f", "s16le", "pipe:1"
            );

            builder.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = builder.start();

            this.audioProcess = process;

            Thread thread = new Thread(() -> this.runAudio(process.getInputStream()), "BBS Banner Sound");

            thread.setDaemon(true);
            thread.start();
        }

        private void runAudio(InputStream stream)
        {
            SourceDataLine device = null;

            try
            {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);

                device = AudioSystem.getSourceDataLine(format);
                device.open(format, SAMPLE_RATE / 5 * FRAME_BYTES);
                device.start();

                this.line = device;

                /* Small chunks, so the count of what was handed over stays close to the truth */
                byte[] bytes = new byte[1024];
                int read;

                while (!this.stop && (read = stream.read(bytes)) > 0)
                {
                    float volume = this.volume;

                    for (int i = 0; i + 1 < read; i += 2)
                    {
                        short sample = (short) ((bytes[i] & 0xFF) | (bytes[i + 1] << 8));

                        sample = (short) (sample * volume * volume);
                        bytes[i] = (byte) sample;
                        bytes[i + 1] = (byte) (sample >> 8);
                    }

                    int length = read - (read % FRAME_BYTES);

                    this.written += length;
                    device.write(bytes, 0, length);
                }
            }
            catch (Exception e)
            {
                /* No sound device that takes this format: the picture goes on by the wall clock */
            }
            finally
            {
                if (this.written == 0 || !this.stop)
                {
                    this.audioDead = true;
                }

                if (device != null)
                {
                    device.close();
                }
            }
        }

        private double fps = DEFAULT_FPS;

        /** What ffmpeg opens: the file, or the address. */
        private String resolveInput()
        {
            if (BackdropVideo.this.file != null)
            {
                return BackdropVideo.this.file.getAbsolutePath();
            }

            return BackdropVideo.this.url;
        }

        /** The video's own size and frame rate, read off ffmpeg's info output; null (reason in the error) when it can't be. */
        private int[] probe(String ffmpeg, String input) throws Exception
        {
            ProcessBuilder builder = new ProcessBuilder(ffmpeg, "-hide_banner", "-i", input);

            builder.redirectErrorStream(true);

            Process probe = builder.start();
            String output;

            try (InputStream stream = probe.getInputStream())
            {
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            finally
            {
                probe.destroy();
            }

            Matcher size = SIZE_PATTERN.matcher(output);

            if (!size.find())
            {
                BackdropVideo.this.error = "ffmpeg không đọc được file video này";

                return null;
            }

            int w = Integer.parseInt(size.group(1));
            int h = Integer.parseInt(size.group(2));

            Matcher length = DURATION_PATTERN.matcher(output);

            if (length.find())
            {
                BackdropVideo.this.duration = Integer.parseInt(length.group(1)) * 3600.0 + Integer.parseInt(length.group(2)) * 60.0 + Double.parseDouble(length.group(3));
            }

            Matcher rate = FPS_PATTERN.matcher(output);

            this.fps = DEFAULT_FPS;

            if (rate.find())
            {
                double parsed = Double.parseDouble(rate.group(1));

                if (parsed >= 1)
                {
                    this.fps = Math.min(MAX_FPS, parsed);
                }
            }

            if (w <= 0 || h <= 0)
            {
                BackdropVideo.this.error = "Kích thước video không hợp lệ";

                return null;
            }

            return new int[] {w, h};
        }
    }
}
