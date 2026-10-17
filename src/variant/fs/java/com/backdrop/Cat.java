package com.backdrop;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.ui.utils.icons.Icon;

/** Starts a settings category; the BBS builds differ in whether a category carries an icon. */
final class Cat
{
    static void make(SettingsBuilder builder, String id, Icon icon)
    {
        builder.category(id, icon);
    }
}
