package com.backdrop.client;

import com.backdrop.ui.UIBackdropPanel;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.lang.reflect.Field;

/**
 * Backdrop's settings are opened from the banner itself: a small button in its corner, left of BBS's
 * own. Found by the field's name, so a BBS without it just gets no button instead of a crash.
 */
public class BannerButton
{
    public static void add(Object screen)
    {
        try
        {
            Field field = screen.getClass().getDeclaredField("banner");

            field.setAccessible(true);

            UIElement banner = (UIElement) field.get(screen);
            UIElement owner = (UIElement) screen;

            UIIcon button = new UIIcon(Icons.PLAY, (b) -> UIOverlay.addOverlay(owner.getContext(), new UIBackdropPanel(), 300, 440));

            button.tooltip(L10n.lang("backdrop.ui.title"));
            button.relative(banner).x(1F, -52).y(8).wh(20, 20);
            banner.add(button);
        }
        catch (Throwable e)
        {
            System.err.println("[Backdrop] No button on the banner: " + e);
        }
    }
}
