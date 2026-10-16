# Backdrop (test builds)

Public addon for BBS: video **with sound** behind BBS, and **Spotify** in it.
Addon cho BBS (bản gốc, FS, RE): video có tiếng làm nền, kèm Spotify.

## Works on
BBS (mchorse) 2.0+, BBS FS 2.5.2–2.7 and BBS-RE, Fabric 1.20.4. The core is a plain Fabric mod: two mixins into classes every BBS has
(`UIDashboard.background` for the wallpaper — film editor included — and `UILandingScreen.renderBanner` for the banner, where it exists)
and its own `config/backdrop.json`. Where BBS has the addon API (FS, RE) it also adds a **Backdrop** panel to the dashboard and its
own language files (English, Tiếng Việt); the panel edits the same config.

## Use
1. Put the jar in `mods/` (needs Fabric API and ffmpeg: BBS's own ffmpeg path is reused, or `/backdrop ffmpeg <path>`). Videos (`.mp4`, H.264, ≤720p) go in BBS `assets/video/`.
2. Pick them in the **Backdrop** panel (where BBS has the addon API), or by command on any BBS:
```
/backdrop videos
/backdrop wallpaper video <file> | url <mp4 link> | on | off | clear | volume 0-1 | sync <ms>
/backdrop banner    video <file> | url <mp4 link> | on | off | clear | volume 0-1 | sync <ms>
/backdrop darkness 0-1 · /backdrop panels 0-1
```

## Spotify
Spotify cannot be played by a mod; this is a remote for the app you already have open. Reading works for any account, controls need Premium.
1. developer.spotify.com/dashboard → Create app, Redirect URI `http://127.0.0.1:8977/callback`, "Web API".
2. In the Backdrop panel (or `/backdrop spotify id <ID>` then `/backdrop spotify login`): paste the Client ID, sign in.
3. Start a song in Spotify: a card appears in the corner of BBS screens (`<<  ||  >>`).

## Build
`./gradlew publishToMavenLocal` in BBS-RE, then `./gradlew build` here (GitHub Actions does it and attaches the jar to a pre-release).
