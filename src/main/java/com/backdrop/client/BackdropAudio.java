package com.backdrop.client;

import com.backdrop.BackdropConfig;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.InputStream;

/**
 * The sound of the banner / wallpaper video, played on its own: a looping ffmpeg decoding to a sound
 * device on a thread of its own, with no tie to the picture. A picture that stutters does not slow it,
 * and it keeps going on every BBS screen (the film list, the editors) instead of stopping with the
 * banner — it ends a few seconds after the last BBS screen is closed, or when the volume is 0.
 */
public class BackdropAudio
{
    private static final int SAMPLE_RATE = 44100;
    private static final int FRAME_BYTES = 4;
    private static final long IDLE_NANOS = 3_000_000_000L;
    private static final long RETRY_MS = 5000;

    private static volatile long lastKeep;
    private static volatile float volume;
    private static volatile String wantedKey = "";
    private static volatile String wantedInput = "";
    private static Thread thread;

    /* The device that is playing now, so the picture can follow the sound */
    private static volatile SourceDataLine current;
    private static volatile String currentKey = "";
    private static volatile int generation;

    /** Seconds of sound that have come out of the speakers for this input (counting loops), or -1 when it is not the one playing. */
    public static double position(String key)
    {
        SourceDataLine device = current;

        if (device == null || !key.equals(currentKey) || !device.isOpen())
        {
            return -1;
        }

        return device.getLongFramePosition() / (double) SAMPLE_RATE;
    }

    /** Changes every time the sound starts over, which also starts its position over. */
    public static int generation()
    {
        return generation;
    }

    /** Called every frame a BBS screen is drawn: says that the sound is wanted, and which. */
    public static synchronized void keep()
    {
        BackdropConfig config = BackdropConfig.get();
        BackdropConfig.Source source = null;

        if (config.bannerStyle == 4 && config.banner.volume > 0F && hasSource(config.banner))
        {
            source = config.banner;
        }
        else if (config.wallpaper.enabled && config.wallpaper.volume > 0F && hasSource(config.wallpaper))
        {
            source = config.wallpaper;
        }

        if (source == null)
        {
            wantedKey = "";

            return;
        }

        String input = !source.url.trim().isEmpty() ? source.url.trim() : new File(Assets.videos(), source.video.trim()).getAbsolutePath();

        lastKeep = System.nanoTime();
        volume = source.volume;
        wantedInput = input;
        wantedKey = input;

        if (thread == null || !thread.isAlive())
        {
            thread = new Thread(BackdropAudio::run, "BBS Backdrop Sound");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static boolean hasSource(BackdropConfig.Source source)
    {
        return !source.url.trim().isEmpty() || !source.video.trim().isEmpty();
    }

    private static boolean wanted(String key)
    {
        return key.equals(wantedKey) && System.nanoTime() - lastKeep < IDLE_NANOS;
    }

    private static void run()
    {
        while (System.nanoTime() - lastKeep < IDLE_NANOS)
        {
            String key = wantedKey;

            if (key.isEmpty())
            {
                break;
            }

            play(key, wantedInput);

            if (!wanted(key))
            {
                continue;
            }

            /* Ended by itself (no sound in the file, no device, ffmpeg failed): try again later */
            try
            {
                Thread.sleep(RETRY_MS);
            }
            catch (InterruptedException e)
            {
                return;
            }
        }
    }

    private static void play(String key, String input)
    {
        Process process = null;
        SourceDataLine device = null;

        try
        {
            ProcessBuilder builder = new ProcessBuilder(
                Ffmpeg.path(), "-hide_banner", "-loglevel", "quiet", "-nostdin",
                "-stream_loop", "-1", "-i", input,
                "-vn", "-sn", "-dn", "-map", "0:a:0?",
                "-ac", "2", "-ar", String.valueOf(SAMPLE_RATE), "-f", "s16le", "pipe:1"
            );

            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            process = builder.start();

            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);

            device = AudioSystem.getSourceDataLine(format);
            device.open(format, SAMPLE_RATE / 5 * FRAME_BYTES);
            device.start();
            generation += 1;
            currentKey = key;
            current = device;

            InputStream stream = process.getInputStream();
            byte[] bytes = new byte[2048];
            int read;

            while (wanted(key) && (read = stream.read(bytes)) > 0)
            {
                float level = volume;

                for (int i = 0; i + 1 < read; i += 2)
                {
                    short sample = (short) ((bytes[i] & 0xFF) | (bytes[i + 1] << 8));

                    sample = (short) (sample * level * level);
                    bytes[i] = (byte) sample;
                    bytes[i + 1] = (byte) (sample >> 8);
                }

                device.write(bytes, 0, read - (read % FRAME_BYTES));
            }
        }
        catch (Exception e)
        {
            /* No device or no ffmpeg: silence, and a retry later */
        }
        finally
        {
            if (process != null)
            {
                process.destroy();
            }

            if (device != null)
            {
                if (current == device)
                {
                    current = null;
                }

                device.close();
            }
        }
    }
}
