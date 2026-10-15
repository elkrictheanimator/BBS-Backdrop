# BBS Banner (test builds)

An addon for BBS-RE that plays a **video with sound** in the banner of the landing screen.

**Status: test builds, not public yet.**

## Using it

1. Put the addon jar in `mods/` next to BBS-RE (Fabric 1.20.4).
2. Put a video (`.mp4`, H.264, 720p or less works best) in `assets/video/` of BBS.
3. Open BBS settings, category **bbsbanner**:
   - `video`: the file name, e.g. `banner.mp4`
   - `url`: or a web address — a direct `.mp4` / `.webm` link, or a YouTube link (needs [yt-dlp](https://github.com/yt-dlp/yt-dlp/releases/latest) on the PATH). A link wins over the file.
   - `volume`: 0 to 1. 0 keeps it silent.
   - `sync_offset`: milliseconds. If the sound comes **before** the picture use a positive number; if it comes **after**, a negative one.
4. ffmpeg must be set up in BBS (Settings → video encoder path).

## How the sync works

The sound is the clock. The picture is decoded ahead and the frame whose time has come is shown;
late frames are dropped rather than letting the picture fall behind the sound.

## Building

The build needs BBS-RE in the local Maven: `./gradlew publishToMavenLocal` in BBS-RE, then `./gradlew build` here.
GitHub Actions does both, and attaches the jar to a pre-release on every push to `master`.
