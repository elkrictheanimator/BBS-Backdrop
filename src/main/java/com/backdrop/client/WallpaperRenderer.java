package com.backdrop.client;

import com.backdrop.BackdropConfig;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

import java.lang.reflect.Field;

/** The wallpaper: the video behind every editor, the film editor included. */
public class WallpaperRenderer
{
    private static Field opacity;
    private static boolean opacityLooked;

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
            return false;
        }

        BackdropVideo video = BannerRenderer.source(wallpaper.url, wallpaper.video);

        if (video == null)
        {
            return false;
        }

        Texture frame = video.getFrame(wallpaper.volume, wallpaper.sync);

        if (frame == null || frame.width <= 0 || frame.height <= 0)
        {
            return false;
        }

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

        /* The panels are drawn over it: let it show through them. BBS-RE keeps how opaque they stay in a
         * public field it resets every frame; another BBS without it just keeps solid panels. */
        setPanelOpacity(config.panelOpacity);

        return true;
    }

    private static void setPanelOpacity(float value)
    {
        if (!opacityLooked)
        {
            opacityLooked = true;

            try
            {
                opacity = Class.forName("mchorse.bbs_mod.BBSSettings").getField("surfaceOpacity");
            }
            catch (Exception e)
            {
                opacity = null;
            }
        }

        if (opacity != null)
        {
            try
            {
                opacity.setFloat(null, value);
            }
            catch (Exception e)
            {
                opacity = null;
            }
        }
    }
}
