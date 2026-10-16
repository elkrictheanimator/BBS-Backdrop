package com.backdrop.ui;

import com.backdrop.BackdropConfig;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;
import java.util.function.Consumer;

/** The landing banner's settings (what RE's banner editor has): style, picture and its framing, the two colours, the height. */
public class BannerForm implements BackdropForm.BannerPart
{
    @Override
    public void add(List<UIElement> items, BackdropConfig c, UIElement owner, List<Runnable> cleanup)
    {
        UICirculate style = new UICirculate((b) ->
        {
            c.bannerStyle = b.getValue();
            c.save();
        });

        for (String key : new String[] {"style_default", "style_vertical", "style_horizontal", "style_image", "style_video"})
        {
            style.addLabel(L10n.lang("backdrop.ui." + key));
        }

        style.setValue(c.bannerStyle);
        items.add(style);

        items.add(new UIButton(L10n.lang("backdrop.ui.pick_image"), (b) ->
            UITexturePicker.open(owner.getContext(), c.image, (link) ->
            {
                c.image = link;
                c.bannerStyle = 3;
                style.setValue(3);
                c.save();
            })));
        items.add(new UIButton(L10n.lang("backdrop.ui.remove_image"), (b) ->
        {
            c.image = null;

            if (c.bannerStyle == 3)
            {
                c.bannerStyle = 0;
                style.setValue(0);
            }

            c.save();
        }));

        UIBannerFramer framer = new UIBannerFramer();

        framer.h(Math.max(40, 240 * Math.max(1, c.height) / 440));
        items.add(framer);
        /* The same framing as numbers, for when the wheel and the drag are not handy */
        items.add(BackdropForm.label("backdrop.ui.zoom"));
        items.add(this.framing(c.zoom, 1F, 4F, (v) -> c.zoom = v));
        items.add(BackdropForm.label("backdrop.ui.focus_x"));
        items.add(this.framing(c.focusX, 0F, 1F, (v) -> c.focusX = v));
        items.add(BackdropForm.label("backdrop.ui.focus_y"));
        items.add(this.framing(c.focusY, 0F, 1F, (v) -> c.focusY = v));
        items.add(new UIButton(L10n.lang("backdrop.ui.reframe"), (b) ->
        {
            c.zoom = 1F;
            c.focusX = 0.5F;
            c.focusY = 0.5F;
            c.save();
        }));

        UIColor start = new UIColor((color) ->
        {
            c.colorStart = color & Colors.RGB;
            c.save();
        });
        UIColor end = new UIColor((color) ->
        {
            c.colorEnd = color & Colors.RGB;
            c.save();
        });

        start.setColor(c.colorStart & Colors.RGB);
        end.setColor(c.colorEnd & Colors.RGB);
        items.add(BackdropForm.label("backdrop.ui.color_start"));
        items.add(start);
        items.add(BackdropForm.label("backdrop.ui.color_end"));
        items.add(end);

        items.add(BackdropForm.label("backdrop.ui.height"));

        UITrackpad height = new UITrackpad((v) ->
        {
            c.height = v.intValue();
            c.save();
        });

        height.limit(90, 300).integer();
        height.setValue(c.height);
        items.add(height);

        /* The colour pickers live next to the overlay, so they would outlive the panel */
        cleanup.add(() ->
        {
            start.picker.removeFromParent();
            end.picker.removeFromParent();
        });
    }

    private UITrackpad framing(float value, float min, float max, Consumer<Float> set)
    {
        UITrackpad trackpad = new UITrackpad((v) ->
        {
            set.accept(v.floatValue());
            BackdropConfig.peek().save();
        });

        trackpad.limit(min, max);
        trackpad.setValue(value);

        return trackpad;
    }
}
