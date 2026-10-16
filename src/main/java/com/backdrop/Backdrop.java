package com.backdrop;

import com.backdrop.client.BackdropCommands;
import com.backdrop.spotify.SpotifyCard;
import com.backdrop.spotify.SpotifyCard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * BBS Backdrop: video (with sound) behind BBS, and Spotify in it.
 *
 * <p>A Fabric mod that works on any BBS (mchorse 2.0+, FS 2.5.2+, BBS-RE): it reaches into BBS only
 * through mixins on a few classes every BBS has and keeps its settings in its own file, edited with
 * /backdrop commands. When the BBS has the addon API ({@link BackdropClient}) it adds a dashboard panel
 * and language files on top — the same settings, nicer to edit.</p>
 */
public class Backdrop implements ClientModInitializer
{
    public static final String MOD_ID = "backdrop";

    @Override
    public void onInitializeClient()
    {
        BackdropConfig.get();
        SpotifyCard.register();
        SpotifyCard.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> BackdropCommands.register(dispatcher));
    }
}
