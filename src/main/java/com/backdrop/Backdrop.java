package com.backdrop;

import com.backdrop.client.BackdropCommands;
import com.backdrop.spotify.SpotifyCard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * BBS Backdrop: video (with sound) behind BBS, and Spotify in it.
 *
 * <p>A plain Fabric mod that reaches into BBS only through mixins on a few classes every BBS has, and
 * keeps its settings in its own file — so it does not depend on any BBS version's addon API.</p>
 */
public class Backdrop implements ClientModInitializer
{
    public static final String MOD_ID = "backdrop";

    @Override
    public void onInitializeClient()
    {
        BackdropConfig.get();
        SpotifyCard.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> BackdropCommands.register(dispatcher));
    }
}
