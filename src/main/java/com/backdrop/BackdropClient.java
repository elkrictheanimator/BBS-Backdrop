package com.backdrop;

import com.backdrop.spotify.SpotifyCard;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.BBSClientReadyEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;

import java.util.Collections;

/** Backdrop, the client half (entry point {@code bbs-client-addon}). */
public class BackdropClient implements BBSAddonMod
{
    /** The language files in this jar: assets/backdrop/assets/strings/&lt;lang&gt;.json, loaded with BBS's own. */
    @Subscribe
    public void onL10n(RegisterL10nEvent event)
    {
        event.l10n.register((lang) -> Collections.singletonList(Link.create(BackdropAddon.MOD_ID + ":strings/" + lang + ".json")));
    }

    @Subscribe
    public void onClientReady(BBSClientReadyEvent event)
    {
        SpotifyCard.register();
    }
}
