package com.backdrop.spotify;

import com.backdrop.BackdropConfig;
import com.backdrop.Lang;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The little "now playing" card in the corner of BBS's screens. Hooked with Fabric's screen events
 * rather than a mixin, so it does not depend on which BBS version is installed: it only looks at
 * whether the screen is BBS's {@code UIScreen}.
 */
public class SpotifyCard
{
    private static final int W = 224;
    private static final int H = 56;
    private static final int MARGIN = 8;
    private static final Identifier COVER = new Identifier("backdrop", "spotify_cover");

    private static NativeImageBackedTexture cover;

    public static void register()
    {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
        {
            if (!isBBS(screen))
            {
                return;
            }

            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, delta) -> draw(context, s.width, s.height, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> !click(s.width, s.height, mouseX, mouseY, button));
        });
    }

    private static boolean isBBS(Screen screen)
    {
        return screen.getClass().getName().equals("mchorse.bbs_mod.ui.framework.UIScreen");
    }

    private static boolean shown()
    {
        return BackdropConfig.get().spotify.card && Spotify.signedIn();
    }

    private static int x(int screenWidth)
    {
        return screenWidth - W - MARGIN;
    }

    private static int y(int screenHeight)
    {
        return screenHeight - H - MARGIN;
    }

    private static void updateCover(MinecraftClient client)
    {
        BufferedImage image = Spotify.pendingCover;

        if (image == null)
        {
            return;
        }

        Spotify.pendingCover = null;

        BufferedImage scaled = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, 64, 64, null);
        g.dispose();

        NativeImage native_ = new NativeImage(64, 64, false);

        for (int py = 0; py < 64; py++)
        {
            for (int px = 0; px < 64; px++)
            {
                int argb = scaled.getRGB(px, py);
                int abgr = (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);

                native_.setColor(px, py, abgr);
            }
        }

        if (cover == null)
        {
            cover = new NativeImageBackedTexture(native_);
            client.getTextureManager().registerTexture(COVER, cover);
        }
        else
        {
            cover.setImage(native_);
            cover.upload();
        }
    }

    private static void draw(DrawContext context, int sw, int sh, int mouseX, int mouseY)
    {
        if (!shown())
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        int x = x(sw);
        int y = y(sh);

        Spotify.wanted();
        updateCover(client);

        context.fill(x, y, x + W, y + H, 0xCC101010);
        context.fill(x, y, x + 2, y + H, 0xFF1DB954);

        if (cover != null && Spotify.active)
        {
            context.drawTexture(COVER, x + 6, y + 4, 48, 48, 0, 0, 64, 64, 64, 64);
        }
        else
        {
            context.fill(x + 6, y + 4, x + 54, y + 52, 0xFF202020);
        }

        int tx = x + 60;
        int tw = W - 66;

        if (!Spotify.active || Spotify.title.isEmpty())
        {
            String message = Spotify.status.isEmpty() ? Lang.tr("Spotify: nothing playing", "Spotify: chưa phát gì") : Spotify.status;

            context.drawTextWithShadow(font, font.trimToWidth(message, tw), tx, y + 10, 0xFFFFFFFF);
        }
        else
        {
            context.drawTextWithShadow(font, font.trimToWidth(Spotify.title, tw), tx, y + 6, 0xFFFFFFFF);
            context.drawTextWithShadow(font, font.trimToWidth(Spotify.artist, tw), tx, y + 17, 0xFFAAAAAA);

            /* The progress line */
            long duration = Spotify.durationMs;
            int barY = y + 30;

            context.fill(tx, barY, tx + tw, barY + 2, 0xFF404040);

            if (duration > 0)
            {
                int filled = (int) (tw * (double) Spotify.progress() / duration);

                context.fill(tx, barY, tx + Math.max(0, Math.min(tw, filled)), barY + 2, 0xFF1DB954);
            }
        }

        /* Buttons: previous, play / pause, next */
        String[] labels = {"<<", Spotify.playing ? "||" : ">", ">>"};

        for (int i = 0; i < 3; i++)
        {
            int bx = tx + i * 26;
            int by = y + 38;
            boolean hover = mouseX >= bx && mouseX < bx + 24 && mouseY >= by && mouseY < by + 14;

            context.fill(bx, by, bx + 24, by + 14, hover ? 0xFF3A3A3A : 0xFF252525);
            context.drawCenteredTextWithShadow(font, labels[i], bx + 12, by + 3, 0xFFFFFFFF);
        }

        if (Spotify.volume >= 0)
        {
            context.drawTextWithShadow(font, Spotify.volume + "%", tx + 84, y + 41, 0xFF888888);
        }
    }

    /** True when the click was on the card (and so is not for BBS). */
    private static boolean click(int sw, int sh, double mouseX, double mouseY, int button)
    {
        if (!shown())
        {
            return false;
        }

        int x = x(sw);
        int y = y(sh);

        if (mouseX < x || mouseX >= x + W || mouseY < y || mouseY >= y + H)
        {
            return false;
        }

        if (button != 0)
        {
            return true;
        }

        int tx = x + 60;
        int by = y + 38;

        if (mouseY >= by && mouseY < by + 14)
        {
            int index = (int) ((mouseX - tx) / 26);

            if (index >= 0 && index < 3 && (mouseX - tx) % 26 < 24)
            {
                if (index == 0) Spotify.previous();
                else if (index == 1) Spotify.playPause();
                else Spotify.next();
            }
        }

        return true;
    }
}
