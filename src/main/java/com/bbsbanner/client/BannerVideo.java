package com.bbsbanner.client;

import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.utils.FFMpegUtils;
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
public class BannerVideo
{
    private static final Map<String, BannerVideo> VIDEOS = new HashMap<>();

    private static final Pattern SIZE_PATTERN = Pattern.compile("Video:.*?(\\d{2,5})x(\\d{2,5})");
    private static final Pattern FPS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?) fps");

    private static final int MAX_WIDTH = 854;
    private static final double MAX_FPS = 60;
    private static final double DEFAULT_FPS = 30;

    private static final int SAMPLE_RATE = 44100;
    private static final int FRAME_BYTES = 4;

    /* Frames decoded ahead of the clock: enough to ride out a hiccup, few enough to stay lean */
    private static final int QUEUE = 6;

    private static final long IDLE_NANOS = 3_000_000_000L;
    private static final long RETRY_NANOS = 5_000_000_000L;
    private static final long AUDIO_WAIT_NANOS = 2_000_000_000L;

    private final File file;
    private final String url;

    private Texture texture;
    private Session session;
    private volatile long lastUsed;
    private volatile long failedAt;
    private volatile String error;

    public static BannerVideo get(String url, File file)
    {
        String key = url != null ? url : file.getAbsolutePath();

        synchronized (VIDEOS)
        {
            return VIDEOS.computeIfAbsent(key, (k) -> new BannerVideo(url == null ? file : null, url));
        }
    }

    private BannerVideo(File file, String url)
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

        boolean wantSound = volume > 0F;
        Session current = this.session;

        /* The sound switched on or off, or the session ended: start a fresh one */
        if (current != null && (current.sound != wantSound || current.finished))
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

        current.volume = volume;

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

        public Session(boolean sound)
        {
            this.sound = sound;
            this.volume = 1F;
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

        /** The time, in seconds, the picture should be at: the sound's position while it plays, else the wall clock. */
        public double clock()
        {
            double time;
            long now = System.nanoTime();
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
                String ffmpeg = FFMpegUtils.getFFMPEG();
                String input = this.resolveInput();

                if (input == null)
                {
                    return;
                }

                double fps = DEFAULT_FPS;
                int[] size = this.probe(ffmpeg, input);

                if (size == null)
                {
                    return;
                }

                fps = this.fps;

                int w = Math.max(2, Math.min(size[0], MAX_WIDTH) & ~1);
                int h = Math.max(2, (int) Math.round(size[1] * (w / (double) size[0])) & ~1);

                this.width = w;
                this.height = h;

                for (int i = 0; i < QUEUE + 3; i++)
                {
                    this.free.offer(ByteBuffer.allocateDirect(w * h * 4));
                }

                ProcessBuilder builder = new ProcessBuilder(
                    ffmpeg, "-hide_banner", "-loglevel", "quiet", "-nostdin",
                    "-stream_loop", "-1", "-i", input,
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
                    if (System.nanoTime() - BannerVideo.this.lastUsed > IDLE_NANOS)
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
                                BannerVideo.this.error = "ffmpeg không giải mã được video này (thử file .mp4 H.264)";
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
                        if (System.nanoTime() - BannerVideo.this.lastUsed > IDLE_NANOS)
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
                    BannerVideo.this.error = "Không chạy được ffmpeg (đặt đường dẫn ffmpeg trong Settings > Video): " + e.getMessage();
                }
            }
            finally
            {
                this.shutdown();

                if (BannerVideo.this.error != null)
                {
                    BannerVideo.this.failedAt = System.nanoTime();
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

        /** What ffmpeg opens: the file, the address, or what yt-dlp makes of a page (YouTube and the like). */
        private String resolveInput() throws Exception
        {
            if (BannerVideo.this.file != null)
            {
                return BannerVideo.this.file.getAbsolutePath();
            }

            String address = BannerVideo.this.url;
            String lower = address.toLowerCase();
            boolean page = lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("twitch.tv")
                || lower.contains("vimeo.com") || lower.contains("bilibili.com");

            if (!page)
            {
                return address;
            }

            Process process;

            try
            {
                process = new ProcessBuilder("yt-dlp", "-g", "--no-playlist", "-f", "best[height<=720][vcodec!=none]/best[height<=720]/best", address).start();
            }
            catch (Exception e)
            {
                BannerVideo.this.error = "Link này cần yt-dlp: cài yt-dlp rồi thử lại";

                return null;
            }

            String output;

            try (InputStream stream = process.getInputStream())
            {
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            finally
            {
                process.destroy();
            }

            if (output.isEmpty())
            {
                BannerVideo.this.error = "yt-dlp không lấy được video từ link này (link sai, riêng tư, hoặc yt-dlp cũ cần cập nhật)";

                return null;
            }

            return output.split("\\r?\\n")[0];
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
                BannerVideo.this.error = "ffmpeg không đọc được file video này";

                return null;
            }

            int w = Integer.parseInt(size.group(1));
            int h = Integer.parseInt(size.group(2));

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
                BannerVideo.this.error = "Kích thước video không hợp lệ";

                return null;
            }

            return new int[] {w, h};
        }
    }
}
