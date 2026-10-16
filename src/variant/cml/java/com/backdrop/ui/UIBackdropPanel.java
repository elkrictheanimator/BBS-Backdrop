package com.backdrop.ui;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;

/** The Backdrop window as a panel of CML's dashboard. */
public class UIBackdropPanel extends UIDashboardPanel
{
    public UIBackdropPanel(UIDashboard dashboard)
    {
        super(dashboard);

        BackdropForm form = new BackdropForm(this, null);

        form.view().relative(this).xy(0, 0).w(1F).h(1F);
        this.add(form.view());
    }
}
