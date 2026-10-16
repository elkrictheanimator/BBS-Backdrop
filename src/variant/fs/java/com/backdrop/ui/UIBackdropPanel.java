package com.backdrop.ui;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;

/** The Backdrop window, opened from the button on the landing screen's banner. */
public class UIBackdropPanel extends UIOverlayPanel
{
    public UIBackdropPanel()
    {
        super(L10n.lang("backdrop.ui.title"));

        BackdropForm form = new BackdropForm(this, new BannerForm());

        form.view().relative(this.content).xy(0, 0).w(1F).h(1F);
        this.content.add(form.view());

        this.onClose((e) -> form.cleanup.forEach(Runnable::run));
    }
}
