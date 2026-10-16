package com.backdrop.client;

import com.backdrop.BackdropConfig;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

import java.lang.reflect.Field;

/** The wallpaper: the video behind every editor, the film editor included. */
public class WallpaperRenderer
{
    /**
     * Draw the wallpaper over the whole screen, covering it and cropping what overflows, darkened a
     * little so the panels stay readable. Returns false when there is nothing to draw (off, no video,
     * or no frame yet) so BBS draws its own background.
     */
    public static boolean draw(Batcher2D batcher, int w, int h)
    {
        BackdropConfig config = BackdropConfig.get();
        BackdropConfig.Source wallpaper = config.wallpaper;

        if (!wallpaper.enabled || w <= 0 || h <= 0)
        {
            note("wallpaper off (enabled=" + wallpaper.enabled + ")");

            return false;
        }

        BackdropVideo video = VideoSources.source(wallpaper.url, wallpaper.video);

        if (video == null)
        {
            note("no wallpaper video chosen");

            return false;
        }

        Texture frame = video.getFrame(wallpaper.volume, wallpaper.sync);

        if (frame == null || frame.width <= 0 || frame.height <= 0)
        {
            note("waiting for the first frame" + (video.getError() == null ? "" : ": " + video.getError()));

            return false;
        }

        note("drawing the wallpaper " + frame.width + "x" + frame.height);

        float areaAspect = (float) w / h;
        float textureAspect = (float) frame.width / frame.height;
        float cropW = frame.width;
        float cropH = frame.height;

        if (areaAspect > textureAspect)
        {
            cropH = frame.width / areaAspect;
        }
        else
        {
            cropW = frame.height * areaAspect;
        }

        float u1 = (frame.width - cropW) * 0.5F;
        float v1 = (frame.height - cropH) * 0.5F;

        batcher.texturedBox(frame, 0xFFFFFFFF, 0, 0, w, h, u1, v1, u1 + cropW, v1 + cropH, frame.width, frame.height);
        batcher.box(0, 0, w, h, ((int) (Math.max(0F, Math.min(1F, config.darkness)) * 255F)) << 24);

        /* The panels are drawn over it: let it show through them (see BBSSettingsMixin) */
        opacity = config.panelOpacity;
        drawnAt = System.nanoTime();

        return true;
    }

    private static String lastNote = "";

    /** One line in the log each time the wallpaper's state changes, so a report says why it is not there. */
    private static void note(String text)
    {
        if (!text.equals(lastNote))
        {
            lastNote = text;
            System.out.println("[BBS Backdrop] " + text);
        }
    }

    private static volatile float opacity = 1F;
    private static volatile long drawnAt;
    private static int sunken = Integer.MIN_VALUE;

    /** How opaque BBS's surfaces stay: below 1 while a wallpaper was drawn this frame, else 1. */
    public static float panelAlpha()
    {
        return System.nanoTime() - drawnAt < 200_000_000L ? opacity : 1F;
    }

    /** The surface level BBS keeps solid (the timeline's floor). */
    public static int sunkenLevel()
    {
        if (sunken == Integer.MIN_VALUE)
        {
            try
            {
                Field field = Class.forName("mchorse.bbs_mod.BBSSettings").getDeclaredField("SURFACE_SUNKEN");

                field.setAccessible(true);
                sunken = field.getInt(null);
            }
            catch (Throwable e)
            {
                sunken = -1;
            }
        }

        return sunken;
    }
}
