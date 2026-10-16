package com.backdrop;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSettingsEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * BBS FS / BBS-RE: Backdrop's part that uses BBS's addon API (entry point {@code bbs-addon}): its own
 * assets, so the language files can be found, and its category in BBS's settings screen.
 */
public class BackdropAddon implements BBSAddonMod
{
    public static final String MOD_ID = BackdropSettings.MOD_ID;

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
        event.registerAddon(MOD_ID, BackdropAddon.class);
    }

    @Subscribe
    public void onSettings(RegisterSettingsEvent event)
    {
        event.register(Icons.PLAY, MOD_ID, (builder) -> BackdropSettings.register(builder, true));
    }
}
