package com.bbsbanner;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSettingsEvent;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * BBS Banner: a video (with sound) for the banner of the BBS landing screen.
 *
 * <p>The settings live in their own category of BBS's settings screen. The drawing is a mixin into
 * the landing screen ({@link com.bbsbanner.mixin.UILandingScreenMixin}), because BBS's addon API has
 * no hook for it yet.</p>
 */
public class BannerAddon implements BBSAddonMod
{
    public static final String MOD_ID = "bbsbanner";

    /** Plays the video in the banner when on; off hands the banner back to BBS untouched. */
    public static ValueBoolean enabled;

    /** A video file in the assets' {@code video} folder (e.g. {@code banner.mp4}). */
    public static ValueString video;

    /** A web address instead of a file: a direct .mp4 / .webm link, or YouTube and the like (needs yt-dlp). Wins over the file. */
    public static ValueString url;

    /** The sound's volume, 0 to 1; 0 keeps the video silent (and the sound undecoded). */
    public static ValueFloat volume;

    /** Milliseconds: positive shows the picture ahead of the sound, negative behind it. */
    public static ValueInt syncOffset;

    @Subscribe
    public void onSettings(RegisterSettingsEvent event)
    {
        event.register(Icons.GEAR, MOD_ID, (builder) ->
        {
            builder.category("general", Icons.GEAR);

            enabled = builder.getBoolean("enabled", true);
            video = builder.getString("video", "");
            url = builder.getString("url", "");
            volume = builder.getFloat("volume", 0.5F, 0F, 1F);
            syncOffset = builder.getInt("sync_offset", 0, -2000, 2000);
        });
    }
}
