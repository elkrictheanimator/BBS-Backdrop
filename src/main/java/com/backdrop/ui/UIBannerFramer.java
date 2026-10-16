package com.backdrop.ui;

import com.backdrop.BackdropConfig;
import com.backdrop.client.BannerRenderer;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.l10n.L10n;

/**
 * A small copy of the banner to frame the picture on by hand: drag with the left mouse button to
 * slide the picture, turn the wheel to zoom.
 */
public class UIBannerFramer extends UIElement
{
    private static final float MAX_ZOOM = 4F;

    private final float aspect;

    private boolean dragging;
    private int lastX;
    private int lastY;

    public UIBannerFramer(float aspect)
    {
        super();

        this.aspect = aspect;
    }

    private Texture texture()
    {
        BackdropConfig config = BackdropConfig.peek();

        return config.image == null ? null : BBSModClient.getTextures().getTexture(config.image);
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            this.dragging = true;
            this.lastX = context.mouseX;
            this.lastY = context.mouseY;

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        boolean was = this.dragging;

        this.dragging = false;

        return was;
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        if (!this.area.isInside(context) || this.texture() == null)
        {
            return super.subMouseScrolled(context);
        }

        BackdropConfig config = BackdropConfig.peek();

        /* Each notch is a tenth, so the zoom feels the same near 1x and near 4x */
        config.zoom = Math.max(1F, Math.min(MAX_ZOOM, config.zoom * (float) Math.pow(1.1, context.mouseWheel)));
        config.save();

        return true;
    }

    private void drag(UIContext context, Texture texture)
    {
        BackdropConfig config = BackdropConfig.peek();
        float[] crop = BannerRenderer.crop(texture, this.aspect);
        float cropW = crop[2] - crop[0];
        float cropH = crop[3] - crop[1];
        float slackW = texture.width - cropW;
        float slackH = texture.height - cropH;
        int dx = context.mouseX - this.lastX;
        int dy = context.mouseY - this.lastY;

        /* The picture follows the hand */
        if (slackW > 0.5F && dx != 0)
        {
            config.focusX = Math.max(0F, Math.min(1F, config.focusX + (-dx * cropW / Math.max(1, this.area.w)) / slackW));
        }

        if (slackH > 0.5F && dy != 0)
        {
            config.focusY = Math.max(0F, Math.min(1F, config.focusY + (-dy * cropH / Math.max(1, this.area.h)) / slackH));
        }

        config.save();

        this.lastX = context.mouseX;
        this.lastY = context.mouseY;
    }

    @Override
    public void render(UIContext context)
    {
        Texture texture = this.texture();
        FontRenderer font = context.batcher.getFont();

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF101014);

        if (texture == null)
        {
            String hint = L10n.lang("backdrop.ui.pick_image_first").get();

            context.batcher.text(hint, this.area.mx(font.getWidth(hint)), this.area.my(font.getHeight()), 0xFFAAAAAA, false);
        }
        else
        {
            if (this.dragging)
            {
                this.drag(context, texture);
            }

            float[] crop = BannerRenderer.crop(texture, this.aspect);

            context.batcher.texturedBox(texture, Colors.WHITE, this.area.x, this.area.y, this.area.w, this.area.h, crop[0], crop[1], crop[2], crop[3], texture.width, texture.height);

            String hint = L10n.lang("backdrop.ui.frame_hint").get();

            context.batcher.textCard(hint, this.area.x + 4, this.area.ey() - font.getHeight() - 6, Colors.WHITE, Colors.A50, 2);
        }

        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), this.area.isInside(context) ? BBSSettings.primaryColor(Colors.A100) : 0x44FFFFFF);

        super.render(context);
    }
}
