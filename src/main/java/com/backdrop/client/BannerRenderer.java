package com.backdrop.client;

import com.backdrop.BackdropConfig;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;

import java.io.File;

/** What the mixin asks for: the video's current frame, covering the banner. */
public class BannerRenderer
{
    private static String problem;

    /**
     * Draw the banner in the chosen style over BBS's; false hands the banner back to BBS (the default
     * slideshow, or a style that has nothing to show yet).
     */
    public static boolean draw(UIContext context, Area area)
    {
        problem = null;

        BackdropConfig config = BackdropConfig.get();

        if (area.w <= 0 || area.h <= 0)
        {
            return false;
        }

        switch (config.bannerStyle)
        {
            case 1:
            case 2:
                int a = Colors.A100 | (config.colorStart & Colors.RGB);
                int b = Colors.A100 | (config.colorEnd & Colors.RGB);

                if (config.bannerStyle == 1)
                {
                    context.batcher.gradientVBox(area.x, area.y, area.ex(), area.ey(), a, b);
                }
                else
                {
                    context.batcher.gradientHBox(area.x, area.y, area.ex(), area.ey(), a, b);
                }

                return true;
            case 3:
                Texture image = config.image == null ? null : BBSModClient.getTextures().getTexture(config.image);

                if (image == null)
                {
                    return false;
                }

                float[] crop = crop(image, (float) area.w / area.h);

                context.batcher.texturedBox(image, 0xFFFFFFFF, area.x, area.y, area.w, area.h, crop[0], crop[1], crop[2], crop[3], image.width, image.height);

                return true;
            case 4:
                return drawVideo(context, area, config.banner);
            default:
                return false;
        }
    }

    private static boolean drawVideo(UIContext context, Area area, BackdropConfig.Source banner)
    {
        BackdropVideo video = source(banner.url, banner.video);

        if (video == null)
        {
            return false;
        }

        Texture frame = video.getFrame(banner.volume, banner.sync);

        if (frame == null || frame.width <= 0 || frame.height <= 0)
        {
            problem = video.getError();

            return false;
        }

        float[] crop = crop(frame, (float) area.w / area.h);

        context.batcher.texturedBox(frame, 0xFFFFFFFF, area.x, area.y, area.w, area.h, crop[0], crop[1], crop[2], crop[3], frame.width, frame.height);

        return true;
    }

    /**
     * The part of a picture a banner of this aspect shows: {u1, v1, u2, v2} in pixels. The picture fills
     * the banner, is zoomed by the setting, and the window slides over it by the focus.
     */
    public static float[] crop(Texture texture, float areaAspect)
    {
        BackdropConfig config = BackdropConfig.get();
        float texW = texture.width;
        float texH = texture.height;
        float cropW;
        float cropH;

        if (areaAspect > texW / texH)
        {
            cropW = texW;
            cropH = texW / areaAspect;
        }
        else
        {
            cropH = texH;
            cropW = texH * areaAspect;
        }

        float zoom = Math.max(1F, config.zoom);

        cropW /= zoom;
        cropH /= zoom;

        float u1 = Math.max(0F, texW - cropW) * Math.max(0F, Math.min(1F, config.focusX));
        float v1 = Math.max(0F, texH - cropH) * Math.max(0F, Math.min(1F, config.focusY));

        return new float[] {u1, v1, u1 + cropW, v1 + cropH};
    }

    /** Written over BBS's own banner when the video can't be played, so it isn't a mystery. */
    public static void drawProblem(UIContext context, Area area)
    {
        if (problem == null)
        {
            return;
        }

        String text = context.batcher.getFont().limitToWidth(problem, Math.max(10, area.w - 16));

        context.batcher.textCard(text, area.x + 8, area.ey() - 18, 0xFFFFFFFF, 0xaa000000);
    }

    /** The video a pair of settings names: the web address wins over the file in the assets' video folder. */
    public static BackdropVideo source(String url, String file)
    {
        url = url == null ? "" : url.trim();

        if (!url.isEmpty())
        {
            return BackdropVideo.get(url, null);
        }

        String name = file == null ? "" : file.trim();

        if (name.isEmpty())
        {
            return null;
        }

        return BackdropVideo.get(null, new File(Assets.videos(), name));
    }
}
