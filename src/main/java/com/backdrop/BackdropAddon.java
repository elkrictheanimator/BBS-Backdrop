package com.backdrop;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;

/**
 * The part of Backdrop that uses BBS's addon API, where there is one (BBS-RE, BBS FS 2.5.2+): its own
 * assets, so the language files can be found. Everything works without it; {@link Backdrop} is the core.
 */
public class BackdropAddon implements BBSAddonMod
{
    public static final String MOD_ID = "backdrop";

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
        event.registerAddon(MOD_ID, BackdropAddon.class);
    }
}
