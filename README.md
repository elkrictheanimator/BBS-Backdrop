# BBS BackDrop

A client-side addon for **BBS FS** that turns the editor into something nicer to sit in front of for hours: a video with its own sound as the wallpaper behind every BBS screen, a fully customizable landing-screen banner, and a Spotify remote card.

Everything is optional and off by default until you pick something. If nothing is set, BBS looks exactly as it did before.

## Features

**Video wallpaper**
- Plays a video behind every BBS screen: the dashboard and its panels, the film editor included.
- The video's **sound plays too**, with its own volume. Picture and sound stay together; the picture follows the sound, so a stutter never leaves them apart.
- Panels become translucent so the wallpaper shows through; you set how dark the veil is and how opaque the panels are.
- The sound pauses (and the picture with it) when no BBS screen is open or the game window is in the background, and resumes where it stopped.

**Landing-screen banner** (BBS FS)
- Styles: BBS's own slideshow, vertical gradient, horizontal gradient, your own picture, or a video.
- Pick any two gradient colors, set the banner height, and frame a picture with zoom and position (drag and mouse wheel, or the sliders).
- **Background around the banner**: BBS's own animated lights, a gradient, your picture, or the wallpaper video.

**Spotify remote card**
- A small card in the corner of BBS screens with the cover, title, artist, and previous / play-pause / next buttons.
- It controls the Spotify app you already have open. Showing what is playing works with any account; the buttons need Spotify Premium (a Spotify limitation).

**Video quality**
- Low (480p, 20 fps), Medium (854p, 30 fps, default) or High (1280p, 60 fps), so weaker PCs can keep the wallpaper smooth.

**Languages:** English and Tiếng Việt.

## Requirements

| | |
|---|---|
| BBS | BBS FS 2.7 (Fabric) |
| Minecraft | 1.20.4 or 1.20.1 |
| Java | 17 or newer |
| Other | Fabric Loader 0.16+, Fabric API, **ffmpeg** |

ffmpeg decodes the video and the sound. If BBS already knows where ffmpeg is, BackDrop uses that; otherwise set the path in BackDrop's settings (General).

## Download

Releases page: pick the jar that matches your Minecraft version.

- `BBS-BackDrop-<version>-fs-1.20.4.jar` for Minecraft 1.20.4
- `BBS-BackDrop-Port-<version>-fs-1.20.1.jar` for Minecraft 1.20.1

Put the jar in your `mods` folder next to BBS and Fabric API.

## How to use

1. **Add a video.** Put an `.mp4` (H.264 works best) in BBS's assets folder under `video/`, i.e. `config/bbs/assets/video/` in most setups. Or paste a direct web address to a video file into the URL field of the window (the address wins over a chosen file).
2. **Open the BackDrop window.** Go to BBS's landing screen and press the **play button on the banner**. The same settings also appear as the *BBS Backdrop* category in BBS's settings screen.
3. **Wallpaper.** Switch it on, pick the video, set the volume and the darkness / panel opacity to taste.
4. **Banner.** Choose a style. For a picture, press *Pick image* and use the framing preview: drag to move, wheel to zoom. For a video, pick it in the banner video section.
5. **Around the banner.** Under *Background around the banner*, choose BBS lights, gradient, picture or wallpaper video.
6. **Sound offset ("sync").** If sound and picture are slightly apart on your machine, nudge the offset in milliseconds. Leave it at 0 otherwise.
7. **Weak PC?** Set *Video quality* to Low. If it is still too heavy, switch the video off in the same window; nothing else changes.

### Spotify

Spotify cannot be played from inside a mod, so this is a remote for the Spotify app on your PC (or phone), which must be open and playing.

1. Go to <https://developer.spotify.com/dashboard>, create an app (type: Web API) and add the Redirect URI `http://127.0.0.1:8977/callback`.
2. Copy its **Client ID** into the Spotify section of the BackDrop window and press *Sign in*. Your browser opens to approve it.
3. Start a song in Spotify. The card appears in the corner of BBS screens. You can hide it with the *Card* toggle.

Your sign-in tokens are stored in BBS's own settings on your computer. The Client ID is yours; nothing goes through any server of the addon.

## Good to know

- Client side only. It changes nothing in worlds and does not need to be installed on a server.
- The sound belongs to the video, so a video without a sound track just plays silently.
- If a video does not play, check that ffmpeg is found and that the file is a normal H.264 `.mp4`. Lines starting with `[BBS Backdrop]` in `logs/latest.log` say what the addon tried.
- Made for BBS FS 2.7. It does not target mchorse's original BBS or BBS CML.

## License

See `LICENSE.md`.
