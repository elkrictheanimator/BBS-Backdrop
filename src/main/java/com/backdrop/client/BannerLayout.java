package com.backdrop.client;

import com.backdrop.BackdropConfig;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.utils.resizers.Flex;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Changes the banner's height. The landing screen lays everything out from constants, so this moves
 * what is under the banner by the difference (the card grows, the lists keep their size), once per
 * change of the setting. Looks the parts up by field name; a BBS without them keeps its layout.
 */
public class BannerLayout
{
    private static final Map<Object, Integer> APPLIED = new WeakHashMap<>();

    public static void apply(Object screen)
    {
        try
        {
            int wanted = BackdropConfig.get().height - BackdropConfig.BANNER_H;
            int applied = APPLIED.getOrDefault(screen, 0);

            if (wanted == applied)
            {
                return;
            }

            int diff = wanted - applied;
            UIElement card = field(screen, "card");
            UIElement banner = field(screen, "banner");

            card.getFlex().h.offset += diff;
            banner.getFlex().h.offset += diff;

            int moved = 0;

            for (IUIElement child : card.getChildren())
            {
                if (child == banner || !(child instanceof UIElement))
                {
                    continue;
                }

                Flex flex = ((UIElement) child).getFlex();

                /* Everything that sits under the banner is placed by an offset past it */
                if (flex.y.offset >= 150)
                {
                    flex.y.offset += diff;
                    moved++;

                    if (flex.h.value != 0F)
                    {
                        flex.h.offset -= diff;
                    }
                }
            }

            System.out.println("[BBS Backdrop] banner height " + (BackdropConfig.BANNER_H + wanted) + " applied to " + moved + " parts");
            APPLIED.put(screen, wanted);
            ((UIElement) screen).resize();
        }
        catch (Throwable e)
        {
            APPLIED.put(screen, BackdropConfig.get().height - BackdropConfig.BANNER_H);
            System.err.println("[Backdrop] Could not change the banner height: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object screen, String name) throws Exception
    {
        Field field = screen.getClass().getDeclaredField(name);

        field.setAccessible(true);

        return (T) field.get(screen);
    }
}
