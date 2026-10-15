package com.bbsbanner.client;

import com.bbsbanner.BannerAddon;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.File;

/** What the mixin asks for: the video's current frame, covering the banner. */
public class BannerRenderer
{
    private static String problem;

    /** Draw the frame over the banner; false when there is nothing to draw yet. */
    public static boolean draw(UIContext context, Area area)
    {
        problem = null;

        if (BannerAddon.enabled == null || !BannerAddon.enabled.get() || area.w <= 0 || area.h <= 0)
        {
            return false;
        }

        BannerVideo video = source();

        if (video == null)
        {
            return false;
        }

        Texture frame = video.getFrame(BannerAddon.volume.get(), BannerAddon.syncOffset.get());

        if (frame == null || frame.width <= 0 || frame.height <= 0)
        {
            problem = video.getError();

            return false;
        }

        float areaAspect = (float) area.w / area.h;
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

        context.batcher.texturedBox(frame, Colors.WHITE, area.x, area.y, area.w, area.h, u1, v1, u1 + cropW, v1 + cropH, frame.width, frame.height);

        return true;
    }

    /** Written over BBS's own banner when the video can't be played, so it isn't a mystery. */
    public static void drawProblem(UIContext context, Area area)
    {
        if (problem == null)
        {
            return;
        }

        String text = context.batcher.getFont().limitToWidth(problem, Math.max(10, area.w - 16));

        context.batcher.textCard(text, area.x + 8, area.ey() - 18, Colors.WHITE, 0xaa000000);
    }

    private static BannerVideo source()
    {
        String url = BannerAddon.url.get().trim();

        if (!url.isEmpty())
        {
            return BannerVideo.get(url, null);
        }

        String name = BannerAddon.video.get().trim();

        if (name.isEmpty())
        {
            return null;
        }

        File file = new File(new File(BBSMod.getAssetsFolder(), "video"), name);

        return BannerVideo.get(null, file);
    }
}
