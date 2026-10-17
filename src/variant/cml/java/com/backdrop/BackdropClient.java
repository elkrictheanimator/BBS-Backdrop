package com.backdrop;

import com.backdrop.ui.UIBackdropPanel;
import mchorse.bbs_mod.addons.BBSClientAddon;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.Collections;

/** BBS CML, the client half (entry point {@code bbs-addon-client}). */
public class BackdropClient extends BBSClientAddon
{
    @Override
    protected void registerL10n(RegisterL10nEvent event)
    {
        event.l10n.register((lang) -> Collections.singletonList(Link.create(BackdropAddon.MOD_ID + ":strings/" + lang + ".json")));
    }

    /** CML's landing page has no banner to hang the window on, so Backdrop gets a panel in the dashboard's bar. */
    @Override
    protected void registerDashboardPanels(RegisterDashboardPanelsEvent event)
    {
        event.dashboard.panels.registerPanel(new UIBackdropPanel(event.dashboard), L10n.lang("backdrop.ui.title"), Icons.PLAY);
    }
}
