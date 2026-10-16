package com.backdrop.mixin;

import com.backdrop.client.WallpaperRenderer;
import mchorse.bbs_mod.utils.colors.Colors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the wallpaper show through the panels: while one was drawn this frame, BBS's surface colours
 * (the panels' backgrounds) come back less opaque. The timeline's floor stays solid.
 */
@Mixin(targets = "mchorse.bbs_mod.BBSSettings", remap = false)
public abstract class BBSSettingsMixin
{
    @Inject(method = "surface", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void backdrop$surface(int level, CallbackInfoReturnable<Integer> cir)
    {
        float alpha = WallpaperRenderer.panelAlpha();

        if (alpha < 1F && level != WallpaperRenderer.sunkenLevel())
        {
            int color = cir.getReturnValue();

            cir.setReturnValue(Colors.setA(color, Colors.getA(color) * alpha));
        }
    }
}
