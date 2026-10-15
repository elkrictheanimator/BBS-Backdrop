package com.bbsbanner.mixin;

import com.bbsbanner.client.BannerRenderer;
import mchorse.bbs_mod.ui.dashboard.panels.landing.UILandingScreen;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the video in the banner of the landing screen. When there is nothing to show (no video set,
 * or it hasn't decoded its first frame yet) the original runs untouched, so BBS's own banner is what
 * stands in; a problem with the video is written over it at the end.
 */
@Mixin(value = UILandingScreen.class, remap = false)
public abstract class UILandingScreenMixin
{
    @Inject(method = "renderBanner", at = @At("HEAD"), cancellable = true, require = 0)
    private void bbsbanner$draw(UIContext context, Area area, CallbackInfo ci)
    {
        if (BannerRenderer.draw(context, area))
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderBanner", at = @At("TAIL"), require = 0)
    private void bbsbanner$problem(UIContext context, Area area, CallbackInfo ci)
    {
        BannerRenderer.drawProblem(context, area);
    }
}
