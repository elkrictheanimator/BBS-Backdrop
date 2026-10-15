package com.backdrop.mixin;

import com.backdrop.client.WallpaperRenderer;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the wallpaper where the dashboard draws its background — which every panel that wants one
 * gets, the film editor among them. When there is nothing to show the original runs untouched.
 *
 * <p>Targets BBS's own method by name, not a Minecraft one, so it is not remapped; the same method
 * exists in BBS 2.x and in the FS builds.</p>
 */
@Mixin(targets = "mchorse.bbs_mod.ui.dashboard.UIDashboard", remap = false)
public abstract class UIDashboardMixin
{
    @Shadow(remap = false)
    public int width;

    @Shadow(remap = false)
    public int height;

    @Inject(method = "background", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void backdrop$wallpaper(UIRenderingContext context, CallbackInfo ci)
    {
        if (WallpaperRenderer.draw(context.batcher, this.width, this.height))
        {
            ci.cancel();
        }
    }
}
