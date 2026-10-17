package com.backdrop.ui;

import com.backdrop.BackdropConfig;
import com.backdrop.client.Assets;
import com.backdrop.spotify.Spotify;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
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
 * What the Backdrop window shows, the same on every BBS: the wallpaper, the Spotify sign-in and, where
 * the BBS has a landing banner, its settings ({@link BannerPart}). Every label is a language key
 * (assets/backdrop/assets/strings). A window (overlay or dashboard panel) puts {@link #view()} in itself.
 */
public class BackdropForm
{
    /** The banner's part of the form, from the build that has a banner. */
    public interface BannerPart
    {
        void add(List<UIElement> items, BackdropConfig config, UIElement owner, List<Runnable> cleanup);
    }

    private final UIElement owner;
    private final UILabel spotifyStatus;
    private final UIElement view;

    /** What to do when the window closes (colour pickers live next to the overlay). */
    public final List<Runnable> cleanup = new ArrayList<>();

    public BackdropForm(UIElement owner, BannerPart banner)
    {
        this.owner = owner;
        this.spotifyStatus = UI.label(IKey.constant(""));

        List<UIElement> items = new ArrayList<>();

        BackdropConfig c = BackdropConfig.get();

        items.add(title("backdrop.ui.wallpaper"));
        this.source(items, c.wallpaper);
        items.add(label("backdrop.ui.darkness"));
        items.add(trackpad(c.darkness, (v) -> c.darkness = v));
        items.add(label("backdrop.ui.panel_opacity"));
        items.add(trackpad(c.panelOpacity, (v) -> c.panelOpacity = v));
        items.add(label("backdrop.ui.quality"));

        UICirculate quality = new UICirculate((b) ->
        {
            c.quality = b.getValue();
            c.save();
        });

        for (String key : new String[] {"quality_low", "quality_medium", "quality_high"})
        {
            quality.addLabel(L10n.lang("backdrop.ui." + key));
        }

        quality.setValue(c.quality);
        items.add(quality);

        if (banner != null)
        {
            items.add(title("backdrop.ui.banner"));
            banner.add(items, c, owner, this.cleanup);
            items.add(title("backdrop.ui.banner_video"));
            this.source(items, c.banner);
        }

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

        this.view = UI.scrollView(5, 10, items.toArray(new UIElement[0]));
        this.refresh();
    }

    public UIElement view()
    {
        return this.view;
    }

    public void refresh()
    {
        this.spotifyStatus.label = L10n.lang(Spotify.signedIn() ? "backdrop.ui.signed_in" : "backdrop.ui.signed_out");
    }

    private static UILabel title(String key)
    {
        return UI.label(L10n.lang(key)).background();
    }

    public static UILabel label(String key)
    {
        return UI.label(L10n.lang(key));
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

            UIOverlay.addOverlay(this.owner.getContext(), panel.set(source.video));
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
            UIOverlay.addOverlay(this.owner.getContext(), panel);
        });
    }
}
