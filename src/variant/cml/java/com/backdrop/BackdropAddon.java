package com.backdrop;

import mchorse.bbs_mod.addons.BBSAddon;
import mchorse.bbs_mod.events.register.RegisterSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * BBS CML: Backdrop's part that uses CML's addon system (entry point {@code bbs-addon}): its own assets,
 * so the language files can be found, and its category in BBS's settings screen.
 */
public class BackdropAddon extends BBSAddon
{
    public static final String MOD_ID = BackdropSettings.MOD_ID;

    @Override
    protected void registerSourcePacks(RegisterSourcePacksEvent event)
    {
        event.provider.register(new InternalAssetsSourcePack(MOD_ID, "assets/" + MOD_ID + "/assets", BackdropAddon.class));
    }

    @Override
    protected void registerSettings(RegisterSettingsEvent event)
    {
        event.register(Icons.PLAY, MOD_ID, (builder) -> BackdropSettings.register(builder, false));
    }
}
