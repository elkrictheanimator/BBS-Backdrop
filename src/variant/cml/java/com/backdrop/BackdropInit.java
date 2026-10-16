package com.backdrop;

import com.backdrop.spotify.SpotifyCard;
import net.fabricmc.api.ClientModInitializer;

/** BBS CML has no "client ready" event for addons, so the Spotify card hooks BBS's screens from here. */
public class BackdropInit implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        SpotifyCard.register();
    }
}
