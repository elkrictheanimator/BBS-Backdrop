package com.backdrop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Everything the addon remembers, in {@code config/backdrop.json}. A plain file on purpose: it
 * needs nothing from BBS, so it reads the same on every BBS version.
 */
public class BackdropConfig
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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

    public Source banner = new Source(0.5F);
    public Source wallpaper = new Source(0F);
    public float darkness = 0.4F;
    public float panelOpacity = 0.72F;

    /** Where ffmpeg is, when BBS can't say (BBS's own setting wins when it is there). */
    public String ffmpeg = "";

    public Spotify spotify = new Spotify();

    /**
     * Where the settings live when BBS has the addon API: its own settings screen (see
     * {@link BackdropAddon}). Set by that class only; without it the JSON file is the truth.
     */
    public interface Bridge
    {
        void pull(BackdropConfig config);

        void push(BackdropConfig config);
    }

    public static volatile Bridge bridge;

    public static synchronized BackdropConfig get()
    {
        if (instance == null)
        {
            instance = load();
        }

        Bridge bridge = BackdropConfig.bridge;

        if (bridge != null)
        {
            bridge.pull(instance);
        }

        return instance;
    }

    private static Path file()
    {
        return FabricLoader.getInstance().getConfigDir().resolve("backdrop.json");
    }

    private static BackdropConfig load()
    {
        try
        {
            Path file = file();

            if (Files.isRegularFile(file))
            {
                BackdropConfig config = GSON.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), BackdropConfig.class);

                if (config != null)
                {
                    config.fix();

                    return config;
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("[Backdrop] Could not read config/backdrop.json: " + e.getMessage());
        }

        return new BackdropConfig();
    }

    /** A file written by hand or by an older version can miss parts. */
    private void fix()
    {
        if (this.banner == null) this.banner = new Source(0.5F);
        if (this.wallpaper == null) this.wallpaper = new Source(0F);
        if (this.spotify == null) this.spotify = new Spotify();
        if (this.banner.video == null) this.banner.video = "";
        if (this.banner.url == null) this.banner.url = "";
        if (this.wallpaper.video == null) this.wallpaper.video = "";
        if (this.wallpaper.url == null) this.wallpaper.url = "";
        if (this.ffmpeg == null) this.ffmpeg = "";
    }

    public synchronized void save()
    {
        Bridge bridge = BackdropConfig.bridge;

        if (bridge != null)
        {
            bridge.push(this);

            return;
        }

        try
        {
            Path file = file();

            Files.createDirectories(file.getParent());
            Files.write(file, GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            System.err.println("[Backdrop] Could not write config/backdrop.json: " + e.getMessage());
        }
    }
}
