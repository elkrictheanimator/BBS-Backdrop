package com.backdrop;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSettingsEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * The part of Backdrop that uses BBS's addon API, where there is one (BBS-RE, BBS FS 2.5.2+): its own
 * assets (the language files) and a category in BBS's settings screen, which then holds the settings
 * the way BBS's own do. Without the API {@link Backdrop} runs on its own file and commands.
 */
public class BackdropAddon implements BBSAddonMod
{
    public static final String MOD_ID = "backdrop";

    private static ValueBoolean bannerEnabled, wallpaperEnabled, card;
    private static ValueString bannerVideo, bannerUrl, wallpaperVideo, wallpaperUrl, ffmpeg, clientId, accessToken, refreshToken, expiresAt;
    private static ValueFloat bannerVolume, wallpaperVolume, darkness, panelOpacity;
    private static ValueInt bannerSync, wallpaperSync;

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
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

            clientId = builder.getString("client_id", "");
            card = builder.getBoolean("card", true);
            accessToken = builder.getString("access_token", "");
            accessToken.invisible();
            refreshToken = builder.getString("refresh_token", "");
            refreshToken.invisible();
            expiresAt = builder.getString("expires_at", "0");
            expiresAt.invisible();

            builder.category("general", Icons.GEAR);

            ffmpeg = builder.getString("ffmpeg", "");

            /* From here on the settings screen is the truth, and the file is left alone */
            BackdropConfig.bridge = new BackdropConfig.Bridge()
            {
                @Override
                public void pull(BackdropConfig c)
                {
                    c.banner.enabled = bannerEnabled.get();
                    c.banner.video = bannerVideo.get();
                    c.banner.url = bannerUrl.get();
                    c.banner.volume = bannerVolume.get();
                    c.banner.sync = bannerSync.get();
                    c.wallpaper.enabled = wallpaperEnabled.get();
                    c.wallpaper.video = wallpaperVideo.get();
                    c.wallpaper.url = wallpaperUrl.get();
                    c.wallpaper.volume = wallpaperVolume.get();
                    c.wallpaper.sync = wallpaperSync.get();
                    c.darkness = darkness.get();
                    c.panelOpacity = panelOpacity.get();
                    c.ffmpeg = ffmpeg.get();
                    c.spotify.clientId = clientId.get();
                    c.spotify.card = card.get();
                    c.spotify.accessToken = accessToken.get();
                    c.spotify.refreshToken = refreshToken.get();

                    try
                    {
                        c.spotify.expiresAt = Long.parseLong(expiresAt.get());
                    }
                    catch (NumberFormatException e)
                    {
                        c.spotify.expiresAt = 0;
                    }
                }

                @Override
                public void push(BackdropConfig c)
                {
                    bannerEnabled.set(c.banner.enabled);
                    bannerVideo.set(c.banner.video);
                    bannerUrl.set(c.banner.url);
                    bannerVolume.set(c.banner.volume);
                    bannerSync.set(c.banner.sync);
                    wallpaperEnabled.set(c.wallpaper.enabled);
                    wallpaperVideo.set(c.wallpaper.video);
                    wallpaperUrl.set(c.wallpaper.url);
                    wallpaperVolume.set(c.wallpaper.volume);
                    wallpaperSync.set(c.wallpaper.sync);
                    darkness.set(c.darkness);
                    panelOpacity.set(c.panelOpacity);
                    ffmpeg.set(c.ffmpeg);
                    clientId.set(c.spotify.clientId);
                    card.set(c.spotify.card);
                    accessToken.set(c.spotify.accessToken);
                    refreshToken.set(c.spotify.refreshToken);
                    expiresAt.set(String.valueOf(c.spotify.expiresAt));
                }
            };
        });
    }
}
