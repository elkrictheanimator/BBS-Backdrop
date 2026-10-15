package com.backdrop;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.BBSApi;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSettingsEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * Backdrop, the common half (entry point {@code bbs-addon}): its settings and its own assets
 * (the language files). Everything client-side is in {@link BackdropClient}.
 *
 * <p>Settings live in BBS's own settings screen, category "backdrop", and are what the Backdrop
 * panel in the dashboard edits too.</p>
 */
public class BackdropAddon implements BBSAddonMod
{
    public static final String MOD_ID = "backdrop";

    /* The banner on the landing screen */
    public static ValueBoolean bannerEnabled;
    public static ValueString bannerVideo;
    public static ValueString bannerUrl;
    public static ValueFloat bannerVolume;
    public static ValueInt bannerSync;

    /* The wallpaper behind the editors */
    public static ValueBoolean wallpaperEnabled;
    public static ValueString wallpaperVideo;
    public static ValueString wallpaperUrl;
    public static ValueFloat wallpaperVolume;
    public static ValueInt wallpaperSync;
    public static ValueFloat darkness;
    public static ValueFloat panelOpacity;

    /* ffmpeg, when BBS's own path is not what should be used */
    public static ValueString ffmpeg;

    /* Spotify */
    public static ValueString spotifyClientId;
    public static ValueBoolean spotifyCard;
    public static ValueString spotifyAccessToken;
    public static ValueString spotifyRefreshToken;
    public static ValueString spotifyExpiresAt;

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
        /* A mismatch should read as "this addon does not fit this BBS", not as a crash later */
        BBSApi.requireVersion(MOD_ID, 1);

        event.registerAddon(MOD_ID, BackdropAddon.class);
    }

    @Subscribe
    public void onSettings(RegisterSettingsEvent event)
    {
        event.register(Icons.PLAY, MOD_ID, (builder) ->
        {
            builder.category("banner", Icons.FILM);

            bannerEnabled = builder.getBoolean("enabled", true);
            bannerVideo = builder.getString("video", "");
            bannerUrl = builder.getString("url", "");
            bannerVolume = builder.getFloat("volume", 0.5F, 0F, 1F);
            bannerSync = builder.getInt("sync", 0, -3000, 3000);

            builder.category("wallpaper", Icons.IMAGE);

            wallpaperEnabled = builder.getBoolean("enabled", true);
            wallpaperVideo = builder.getString("video", "");
            wallpaperUrl = builder.getString("url", "");
            wallpaperVolume = builder.getFloat("volume", 0F, 0F, 1F);
            wallpaperSync = builder.getInt("sync", 0, -3000, 3000);
            darkness = builder.getFloat("darkness", 0.4F, 0F, 1F);
            panelOpacity = builder.getFloat("panel_opacity", 0.72F, 0F, 1F);

            builder.category("spotify", Icons.SOUND);

            spotifyClientId = builder.getString("client_id", "");
            spotifyCard = builder.getBoolean("card", true);
            spotifyAccessToken = builder.getString("access_token", "");
            spotifyAccessToken.invisible();
            spotifyRefreshToken = builder.getString("refresh_token", "");
            spotifyRefreshToken.invisible();
            spotifyExpiresAt = builder.getString("expires_at", "0");
            spotifyExpiresAt.invisible();

            builder.category("general", Icons.GEAR);

            ffmpeg = builder.getString("ffmpeg", "");
        });
    }
}
