package com.backdrop;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * Backdrop's settings, in BBS's own settings screen (category "backdrop"). The same on every BBS the
 * addon is built for; each one's addon class calls {@link #register}.
 */
public class BackdropSettings
{
    public static final String MOD_ID = "backdrop";

    private static ValueBoolean wallpaperEnabled, card;
    private static ValueLink bannerImage, backdropImage;
    private static ValueFloat bannerZoom, bannerFocusX, bannerFocusY;
    private static ValueString bannerVideo, bannerUrl, wallpaperVideo, wallpaperUrl, ffmpeg, clientId, accessToken, refreshToken, expiresAt;
    private static ValueFloat bannerVolume, wallpaperVolume, darkness, panelOpacity;
    private static ValueInt quality, bannerSync, wallpaperSync, backdropStyle, bannerStyle, bannerColorStart, bannerColorEnd, bannerHeight;

    /** The settings, made inside BBS's settings registration; {@code banner}: the BBS has a landing banner to set. */
    public static void register(SettingsBuilder builder, boolean banner)
    {
        if (banner)
        {
            Cat.make(builder, "banner", Icons.FILM);

            bannerStyle = builder.getInt("style", 0, 0, 4);
            bannerColorStart = builder.getInt("color_start", 0x1b2a4e);
            bannerColorEnd = builder.getInt("color_end", 0x8a3ffc);
            bannerImage = builder.getRL("image", null);
            bannerHeight = builder.getInt("height", BackdropConfig.BANNER_H, 90, 300);
            backdropStyle = builder.getInt("backdrop_style", 0, 0, 3);
            backdropImage = builder.getRL("backdrop_image", null);
            backdropStyle.invisible();
            backdropImage.invisible();
            bannerZoom = builder.getFloat("zoom", 1F, 1F, 4F);
            bannerFocusX = builder.getFloat("focus_x", 0.5F, 0F, 1F);
            bannerFocusY = builder.getFloat("focus_y", 0.5F, 0F, 1F);

            /* Edited from the button on the banner, not from the settings list */
            bannerColorStart.invisible();
            bannerColorEnd.invisible();
            bannerImage.invisible();
            bannerZoom.invisible();
            bannerFocusX.invisible();
            bannerFocusY.invisible();
            bannerVideo = builder.getString("video", "");
            bannerUrl = builder.getString("url", "");
            bannerVolume = builder.getFloat("volume", 0.5F, 0F, 1F);
            bannerSync = builder.getInt("sync", 0, -3000, 3000);

        }

            Cat.make(builder, "wallpaper", Icons.IMAGE);

            wallpaperEnabled = builder.getBoolean("enabled", true);
            wallpaperVideo = builder.getString("video", "");
            wallpaperUrl = builder.getString("url", "");
            wallpaperVolume = builder.getFloat("volume", 0F, 0F, 1F);
            wallpaperSync = builder.getInt("sync", 0, -3000, 3000);
            darkness = builder.getFloat("darkness", 0.4F, 0F, 1F);
            panelOpacity = builder.getFloat("panel_opacity", 0.72F, 0F, 1F);

            Cat.make(builder, "spotify", Icons.SOUND);

            clientId = builder.getString("client_id", "");
            card = builder.getBoolean("card", true);
            accessToken = builder.getString("access_token", "");
            accessToken.invisible();
            refreshToken = builder.getString("refresh_token", "");
            refreshToken.invisible();
            expiresAt = builder.getString("expires_at", "0");
            expiresAt.invisible();

            Cat.make(builder, "general", Icons.GEAR);

            quality = builder.getInt("quality", 1, 0, 2);
            ffmpeg = builder.getString("ffmpeg", "");

        /* From here on the settings screen is the truth */
                        BackdropConfig.bridge = new BackdropConfig.Bridge()
            {
                @Override
                public void pull(BackdropConfig c)
                {
                    if (bannerStyle != null)
                    {
                        c.bannerStyle = bannerStyle.get();
                        c.banner.enabled = c.bannerStyle == 4;
                        c.colorStart = bannerColorStart.get();
                        c.colorEnd = bannerColorEnd.get();
                        c.image = bannerImage.get();
                        c.height = bannerHeight.get();
                        c.backdropStyle = backdropStyle.get();
                        c.backdropImage = backdropImage.get();
                        c.zoom = bannerZoom.get();
                        c.focusX = bannerFocusX.get();
                        c.focusY = bannerFocusY.get();
                        c.banner.video = bannerVideo.get();
                        c.banner.url = bannerUrl.get();
                        c.banner.volume = bannerVolume.get();
                        c.banner.sync = bannerSync.get();
                    }

                    c.wallpaper.enabled = wallpaperEnabled.get();
                    c.wallpaper.video = wallpaperVideo.get();
                    c.wallpaper.url = wallpaperUrl.get();
                    c.wallpaper.volume = wallpaperVolume.get();
                    c.wallpaper.sync = wallpaperSync.get();
                    c.darkness = darkness.get();
                    c.panelOpacity = panelOpacity.get();
                    c.quality = quality.get();
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
                    if (bannerStyle != null)
                    {
                        bannerStyle.set(c.bannerStyle);
                        bannerColorStart.set(c.colorStart);
                        bannerColorEnd.set(c.colorEnd);
                        bannerImage.set(c.image);
                        bannerHeight.set(c.height);
                        backdropStyle.set(c.backdropStyle);
                        backdropImage.set(c.backdropImage);
                        bannerZoom.set(c.zoom);
                        bannerFocusX.set(c.focusX);
                        bannerFocusY.set(c.focusY);
                        bannerVideo.set(c.banner.video);
                        bannerUrl.set(c.banner.url);
                        bannerVolume.set(c.banner.volume);
                        bannerSync.set(c.banner.sync);
                    }

                    wallpaperEnabled.set(c.wallpaper.enabled);
                    wallpaperVideo.set(c.wallpaper.video);
                    wallpaperUrl.set(c.wallpaper.url);
                    wallpaperVolume.set(c.wallpaper.volume);
                    wallpaperSync.set(c.wallpaper.sync);
                    darkness.set(c.darkness);
                    panelOpacity.set(c.panelOpacity);
                    quality.set(c.quality);
                    ffmpeg.set(c.ffmpeg);
                    clientId.set(c.spotify.clientId);
                    card.set(c.spotify.card);
                    accessToken.set(c.spotify.accessToken);
                    refreshToken.set(c.spotify.refreshToken);
                    expiresAt.set(String.valueOf(c.spotify.expiresAt));
                }
            };
    }
}
