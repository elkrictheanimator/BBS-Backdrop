package com.backdrop.mixin;

import com.backdrop.client.BannerRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the video in the banner of the landing screen, on the BBS builds that have one with a method
 * of this name (BBS-RE, FS 2.7). On any other the injection finds nothing and does nothing, so the
 * rest of the addon is unaffected. When there is nothing to show the original runs untouched, and a
 * problem with the video is written over it at the end.
 */
@Mixin(targets = "mchorse.bbs_mod.ui.dashboard.panels.landing.UILandingScreen", remap = false)
public abstract class UILandingScreenMixin
{
    @Inject(method = "renderBanner", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void backdrop$draw(UIContext context, Area area, CallbackInfo ci)
    {
        if (BannerRenderer.draw(context, area))
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderBanner", at = @At("TAIL"), remap = false, require = 0)
    private void backdrop$problem(UIContext context, Area area, CallbackInfo ci)
    {
        BannerRenderer.drawProblem(context, area);
    }
}
