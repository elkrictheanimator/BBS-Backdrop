package com.backdrop;

import mchorse.bbs_mod.resources.Link;

/**
 * Backdrop's settings as the drawing code reads them. The truth is BBS's own settings screen
 * ({@link BackdropAddon}); {@link #get()} takes a snapshot of it and {@link #save()} writes the
 * snapshot back.
 */
public class BackdropConfig
{
    private static BackdropConfig instance;

    /** One place a video can play: the landing banner, or the wallpaper behind the editors. */
    public static class Source
    {
        public boolean enabled = true;

        /** A file in BBS's assets/video folder. */
        public String video = "";

        /** A direct .mp4 / .webm link; wins over the file. */
        public String url = "";

        public float volume;

        /** Milliseconds: positive shows the picture ahead of the sound, negative behind it. */
        public int sync;

        public Source(float volume)
        {
            this.volume = volume;
        }
    }

    public static class Spotify
    {
        /** The Client ID of the user's own app at developer.spotify.com (free). */
        public String clientId = "";
        public String accessToken = "";
        public String refreshToken = "";
        public long expiresAt;
        public boolean card = true;
    }

    public static final int BANNER_H = 180;

    public Source banner = new Source(0.5F);

    /** 0 BBS's slideshow, 1 / 2 two-colour gradient (vertical / horizontal), 3 your picture, 4 video. */
    public int bannerStyle;
    public int colorStart = 0x1b2a4e;
    public int colorEnd = 0x8a3ffc;
    public Link image;
    public float zoom = 1F;
    public float focusX = 0.5F;
    public float focusY = 0.5F;
    public int height = BANNER_H;
    public Source wallpaper = new Source(0F);
    public float darkness = 0.4F;
    public float panelOpacity = 0.72F;

    /** Where ffmpeg is, when BBS can't say (BBS's own setting wins when it is there). */
    public String ffmpeg = "";

    public Spotify spotify = new Spotify();

    /** How the snapshot reaches the settings; set by {@link BackdropAddon} once they exist. */
    public interface Bridge
    {
        void pull(BackdropConfig config);

        void push(BackdropConfig config);
    }

    public static volatile Bridge bridge;

    /** The snapshot as it is, without reading the settings again: for editing, so a half-made edit is not undone. */
    public static synchronized BackdropConfig peek()
    {
        if (instance == null)
        {
            instance = new BackdropConfig();
        }

        return instance;
    }

    public static synchronized BackdropConfig get()
    {
        if (instance == null)
        {
            instance = new BackdropConfig();
        }

        Bridge bridge = BackdropConfig.bridge;

        if (bridge != null)
        {
            bridge.pull(instance);
        }

        return instance;
    }

    public synchronized void save()
    {
        Bridge bridge = BackdropConfig.bridge;

        if (bridge != null)
        {
            bridge.push(this);
        }
    }
}
