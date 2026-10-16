package com.backdrop.ui;

import com.backdrop.BackdropConfig;
import com.backdrop.client.Assets;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.utils.colors.Colors;
import com.backdrop.spotify.Spotify;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Backdrop panel, opened from the button on the landing screen's banner: the same values as BBS's settings screen, with pickers for
 * the video and the Spotify sign-in. Every label is a language key (assets/backdrop/assets/strings).
 */
public class UIBackdropPanel extends UIOverlayPanel
{
    private final UILabel spotifyStatus;

    public UIBackdropPanel()
    {
        super(L10n.lang("backdrop.ui.title"));

        this.spotifyStatus = UI.label(IKey.constant(""));

        List<UIElement> items = new ArrayList<>();

        BackdropConfig c = BackdropConfig.get();

        items.add(title("backdrop.ui.wallpaper"));
        this.source(items, c.wallpaper);
        items.add(label("backdrop.ui.darkness"));
        items.add(trackpad(c.darkness, (v) -> c.darkness = v));
        items.add(label("backdrop.ui.panel_opacity"));
        items.add(trackpad(c.panelOpacity, (v) -> c.panelOpacity = v));

        items.add(title("backdrop.ui.banner"));
        this.bannerItems(items, c);
        items.add(title("backdrop.ui.banner_video"));
        this.source(items, c.banner);

        items.add(title("backdrop.ui.spotify"));
        items.add(label("backdrop.ui.spotify_help"));
        items.add(this.textButton("backdrop.ui.client_id", () -> c.spotify.clientId, (v) -> c.spotify.clientId = v));
        items.add(new UIButton(L10n.lang("backdrop.ui.login"), (b) ->
        {
            Spotify.login();
            this.refresh();
        }));
        items.add(new UIButton(L10n.lang("backdrop.ui.logout"), (b) ->
        {
            Spotify.logout();
            this.refresh();
        }));
        items.add(new UIToggle(L10n.lang("backdrop.ui.card"), c.spotify.card, (t) ->
        {
            c.spotify.card = t.getValue();
            c.save();
        }));
        items.add(this.spotifyStatus);

        UIElement column = UI.scrollView(5, 10, items.toArray(new UIElement[0]));

        column.relative(this.content).xy(0, 0).w(1F).h(1F);
        this.content.add(column);

        this.refresh();
    }

    private void refresh()
    {
        this.spotifyStatus.label = L10n.lang(Spotify.signedIn() ? "backdrop.ui.signed_in" : "backdrop.ui.signed_out");
    }

    private static UILabel title(String key)
    {
        return UI.label(L10n.lang(key)).background();
    }

    private static UILabel label(String key)
    {
        return UI.label(L10n.lang(key));
    }

    /** What RE's banner editor has: style, picture and its framing, the two colours, the height. */
    private void bannerItems(List<UIElement> items, BackdropConfig c)
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
            UITexturePicker.open(this.getContext(), c.image, (link) ->
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

        UIBannerFramer framer = new UIBannerFramer((float) 440 / Math.max(1, c.height));

        framer.h(Math.max(40, 240 * Math.max(1, c.height) / 440));
        items.add(framer);
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
        items.add(label("backdrop.ui.color_start"));
        items.add(start);
        items.add(label("backdrop.ui.color_end"));
        items.add(end);

        items.add(label("backdrop.ui.height"));

        UITrackpad height = new UITrackpad((v) ->
        {
            c.height = v.intValue();
            c.save();
        });

        height.limit(90, 300).integer();
        height.setValue(c.height);
        items.add(height);

        /* The colour pickers live next to the overlay, so they would outlive the panel */
        this.onClose((e) ->
        {
            start.picker.removeFromParent();
            end.picker.removeFromParent();
        });
    }

    private void source(List<UIElement> items, BackdropConfig.Source source)
    {
        BackdropConfig c = BackdropConfig.peek();

        if (source == c.wallpaper)
        {
            items.add(new UIToggle(L10n.lang("backdrop.ui.enabled"), source.enabled, (t) ->
            {
                source.enabled = t.getValue();
                c.save();
            }));
        }

        items.add(this.videoButton(source));
        items.add(this.textButton("backdrop.ui.paste_url", () -> source.url, (v) -> source.url = v));
        items.add(label("backdrop.ui.volume"));
        items.add(trackpad(source.volume, (v) -> source.volume = v));
        items.add(label("backdrop.ui.sync"));

        UITrackpad sync = new UITrackpad((v) ->
        {
            source.sync = v.intValue();
            c.save();
        });

        sync.limit(-3000, 3000).integer();
        sync.setValue(source.sync);
        items.add(sync);
    }

    private static UITrackpad trackpad(float value, Consumer<Float> set)
    {
        UITrackpad trackpad = new UITrackpad((v) ->
        {
            set.accept(v.floatValue());
            BackdropConfig.peek().save();
        });

        trackpad.limit(0, 1);
        trackpad.setValue(value);

        return trackpad;
    }

    /** A video of the assets' video folder; picking one clears the web address, which would win over it. */
    private UIButton videoButton(BackdropConfig.Source source)
    {
        return new UIButton(L10n.lang("backdrop.ui.pick_video"), (b) ->
        {
            File[] files = Assets.videos().listFiles();
            List<String> names = new ArrayList<>();

            if (files != null)
            {
                Arrays.sort(files);

                for (File file : files)
                {
                    if (file.isFile())
                    {
                        names.add(file.getName());
                    }
                }
            }

            UIStringOverlayPanel panel = new UIStringOverlayPanel(L10n.lang("backdrop.ui.pick_video"), true, names, (name) ->
            {
                source.video = name == null ? "" : name;

                if (name != null && !name.isEmpty())
                {
                    source.url = "";

                    if (source == BackdropConfig.peek().wallpaper)
                    {
                        source.enabled = true;
                    }
                    else
                    {
                        BackdropConfig.peek().bannerStyle = 4;
                    }
                }

                BackdropConfig.peek().save();
            });

            UIOverlay.addOverlay(this.getContext(), panel.set(source.video));
        });
    }

    private UIButton textButton(String key, Supplier<String> get, Consumer<String> set)
    {
        return new UIButton(L10n.lang(key), (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(L10n.lang(key), L10n.lang(key + "_help"), (text) ->
            {
                set.accept(text == null ? "" : text.trim());
                BackdropConfig.peek().save();
            });

            panel.text.setText(get.get());
            UIOverlay.addOverlay(this.getContext(), panel);
        });
    }
}
