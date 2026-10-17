package com.backdrop.mixin;

import com.backdrop.BackdropConfig;
import com.backdrop.client.WallpaperRenderer;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * What the landing screen sits on, around the banner: BBS draws its lights there; with a style picked
 * in Backdrop's window the two banner colours, a picture or the wallpaper video take their place.
 * Style 0, or anything that cannot be drawn yet, leaves BBS's own untouched.
 */
@Mixin(targets = "mchorse.bbs_mod.ui.dashboard.panels.landing.LandingBackdrop", remap = false)
public abstract class LandingBackdropMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void backdrop$render(UIContext context, Area area, CallbackInfo ci)
    {
        BackdropConfig config = BackdropConfig.get();

        if (config.backdropStyle == 1)
        {
            int top = Colors.A100 | Colors.mulRGB(config.colorStart & Colors.RGB, 0.5F);
            int bottom = Colors.A100 | Colors.mulRGB(config.colorEnd & Colors.RGB, 0.5F);

            context.batcher.gradientVBox(area.x, area.y, area.ex(), area.ey(), top, bottom);
            ci.cancel();
        }
        else if (config.backdropStyle == 2 && config.backdropImage != null)
        {
            Texture texture = BBSModClient.getTextures().getTexture(config.backdropImage);

            if (texture == null || texture.width <= 0 || texture.height <= 0)
            {
                return;
            }

            float areaAspect = (float) area.w / Math.max(1, area.h);
            float textureAspect = (float) texture.width / texture.height;
            float cropW = texture.width;
            float cropH = texture.height;

            if (areaAspect > textureAspect)
            {
                cropH = texture.width / areaAspect;
            }
            else
            {
                cropW = texture.height * areaAspect;
            }

            float u1 = (texture.width - cropW) * 0.5F;
            float v1 = (texture.height - cropH) * 0.5F;

            context.batcher.texturedBox(texture, 0xFFFFFFFF, area.x, area.y, area.w, area.h, u1, v1, u1 + cropW, v1 + cropH, texture.width, texture.height);
            /* A light veil, so the card stays readable over a busy picture */
            context.batcher.box(area.x, area.y, area.ex(), area.ey(), 0x33000000);
            ci.cancel();
        }
        else if (config.backdropStyle == 3 && WallpaperRenderer.draw(context.batcher, area.ex(), area.ey()))
        {
            ci.cancel();
        }
    }
}
