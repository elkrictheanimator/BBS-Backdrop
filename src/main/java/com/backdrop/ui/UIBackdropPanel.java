package com.backdrop.ui;

import com.backdrop.BackdropAddon;
import com.backdrop.client.Assets;
import com.backdrop.spotify.Spotify;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
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

/**
 * The Backdrop panel of the dashboard: the same values as BBS's settings screen, with pickers for
 * the video and the Spotify sign-in. Every label is a language key (assets/backdrop/assets/strings).
 */
public class UIBackdropPanel extends UIDashboardPanel
{
    private final UILabel spotifyStatus;

    public UIBackdropPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.spotifyStatus = UI.label(IKey.constant(""));

        List<UIElement> items = new ArrayList<>();

        items.add(title("backdrop.ui.wallpaper"));
        items.add(toggle("backdrop.ui.enabled", BackdropAddon.wallpaperEnabled));
        items.add(this.videoButton("backdrop.ui.pick_video", BackdropAddon.wallpaperVideo, BackdropAddon.wallpaperUrl));
        items.add(this.urlButton("backdrop.ui.paste_url", BackdropAddon.wallpaperUrl));
        items.add(label("backdrop.ui.volume"));
        items.add(trackpad(BackdropAddon.wallpaperVolume));
        items.add(label("backdrop.ui.sync"));
        items.add(trackpad(BackdropAddon.wallpaperSync));
        items.add(label("backdrop.ui.darkness"));
        items.add(trackpad(BackdropAddon.darkness));
        items.add(label("backdrop.ui.panel_opacity"));
        items.add(trackpad(BackdropAddon.panelOpacity));

        items.add(title("backdrop.ui.banner"));
        items.add(toggle("backdrop.ui.enabled", BackdropAddon.bannerEnabled));
        items.add(this.videoButton("backdrop.ui.pick_video", BackdropAddon.bannerVideo, BackdropAddon.bannerUrl));
        items.add(this.urlButton("backdrop.ui.paste_url", BackdropAddon.bannerUrl));
        items.add(label("backdrop.ui.volume"));
        items.add(trackpad(BackdropAddon.bannerVolume));
        items.add(label("backdrop.ui.sync"));
        items.add(trackpad(BackdropAddon.bannerSync));

        items.add(title("backdrop.ui.spotify"));
        items.add(label("backdrop.ui.spotify_help"));
        items.add(this.textButton("backdrop.ui.client_id", BackdropAddon.spotifyClientId));
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
        items.add(toggle("backdrop.ui.card", BackdropAddon.spotifyCard));
        items.add(this.spotifyStatus);

        UIElement column = UI.scrollView(5, 10, items.toArray(new UIElement[0]));

        column.relative(this).xy(0, 0).w(1F).h(1F);
        this.add(column);

        this.onAppear(this::refresh);
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

    private static UIToggle toggle(String key, ValueBoolean value)
    {
        return new UIToggle(L10n.lang(key), value.get(), (t) -> value.set(t.getValue()));
    }

    private static UITrackpad trackpad(ValueFloat value)
    {
        UITrackpad trackpad = new UITrackpad((v) -> value.set(v.floatValue()));

        trackpad.limit(0, 1);
        trackpad.setValue(value.get());

        return trackpad;
    }

    private static UITrackpad trackpad(ValueInt value)
    {
        UITrackpad trackpad = new UITrackpad((v) -> value.set(v.intValue()));

        trackpad.limit(-3000, 3000).integer();
        trackpad.setValue(value.get());

        return trackpad;
    }

    /** A video of the assets' video folder; picking one clears the web address, which would win over it. */
    private UIButton videoButton(String key, ValueString video, ValueString url)
    {
        return new UIButton(L10n.lang(key), (b) ->
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
                video.set(name == null ? "" : name);

                if (name != null && !name.isEmpty())
                {
                    url.set("");
                }
            });

            UIOverlay.addOverlay(this.getContext(), panel.set(video.get()));
        });
    }

    private UIButton urlButton(String key, ValueString url)
    {
        return this.textButton(key, url);
    }

    private UIButton textButton(String key, ValueString value)
    {
        return new UIButton(L10n.lang(key), (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(L10n.lang(key), L10n.lang(key + "_help"), (text) -> value.set(text == null ? "" : text.trim()));

            panel.text.setText(value.get());
            UIOverlay.addOverlay(this.getContext(), panel);
        });
    }
}
