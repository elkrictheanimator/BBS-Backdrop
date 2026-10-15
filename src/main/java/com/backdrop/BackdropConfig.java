package com.backdrop;

/**
 * A read-only snapshot of Backdrop's settings, taken each time {@link #get()} is called. The truth is in
 * BBS's own settings ({@link BackdropAddon}); this only keeps the drawing code short.
 */
public class BackdropConfig
{
    public static class Source
    {
        public boolean enabled;
        public String video = "";
        public String url = "";
        public float volume;
        public int sync;
    }

    public static class Spotify
    {
        public String clientId = "";
        public boolean card;
    }

    public final Source banner = new Source();
    public final Source wallpaper = new Source();
    public final Spotify spotify = new Spotify();
    public float darkness;
    public float panelOpacity;
    public String ffmpeg = "";

    private static final BackdropConfig INSTANCE = new BackdropConfig();

    public static synchronized BackdropConfig get()
    {
        BackdropConfig c = INSTANCE;

        /* The settings are made while BBS starts; before that nothing is shown */
        if (BackdropAddon.bannerEnabled == null)
        {
            c.banner.enabled = false;
            c.wallpaper.enabled = false;
            c.spotify.card = false;

            return c;
        }

        c.banner.enabled = BackdropAddon.bannerEnabled.get();
        c.banner.video = BackdropAddon.bannerVideo.get();
        c.banner.url = BackdropAddon.bannerUrl.get();
        c.banner.volume = BackdropAddon.bannerVolume.get();
        c.banner.sync = BackdropAddon.bannerSync.get();

        c.wallpaper.enabled = BackdropAddon.wallpaperEnabled.get();
        c.wallpaper.video = BackdropAddon.wallpaperVideo.get();
        c.wallpaper.url = BackdropAddon.wallpaperUrl.get();
        c.wallpaper.volume = BackdropAddon.wallpaperVolume.get();
        c.wallpaper.sync = BackdropAddon.wallpaperSync.get();

        c.darkness = BackdropAddon.darkness.get();
        c.panelOpacity = BackdropAddon.panelOpacity.get();
        c.ffmpeg = BackdropAddon.ffmpeg.get();
        c.spotify.clientId = BackdropAddon.spotifyClientId.get();
        c.spotify.card = BackdropAddon.spotifyCard.get();

        return c;
    }
}
