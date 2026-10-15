# Backdrop (test builds)

An addon for **BBS-RE** (Fabric 1.20.4) built on its addon API: video **with sound** behind BBS, and **Spotify** in it.
Addon cho BBS-RE, dùng API addon của BBS-RE: video có tiếng làm nền, kèm Spotify.

## What it uses from BBS-RE
- `BBSAddonMod` entry points `bbs-addon` (settings, own assets) and `bbs-client-addon` (client)
- `RegisterSettingsEvent`: a **backdrop** category in BBS's settings screen (banner, wallpaper, spotify, general)
- `RegisterL10nEvent` + `RegisterSourcePacksEvent`: its own language files (English, Tiếng Việt) loaded with BBS's
- `RegisterDashboardPanelsEvent`: a **Backdrop** panel in the dashboard's bar
- `BBSClientReadyEvent`, `BBSApi.requireVersion`
- Two mixins, because the API has no hook for drawing a background: `UIDashboard.background` (wallpaper, film editor included) and `UILandingScreen.renderBanner` (banner). If BBS changes them the addon just stops drawing; nothing else breaks.

## Use
1. Put the jar in `mods/` next to BBS-RE. Videos (`.mp4`, H.264, ≤720p) go in BBS `assets/video/`. ffmpeg is the one set in BBS (or `backdrop > general > ffmpeg`).
2. Open the **Backdrop** panel (dashboard bar), pick a video for the wallpaper and/or the banner, set volume and sound offset.
   The same values are in Settings > backdrop.

## Spotify
Spotify cannot be played by a mod; this is a remote for the app you already have open. Reading works for any account, controls need Premium.
1. developer.spotify.com/dashboard → Create app, Redirect URI `http://127.0.0.1:8977/callback`, "Web API".
2. In the Backdrop panel: paste the Client ID, press *Sign in*.
3. Start a song in Spotify: a card appears in the corner of BBS screens (`<<  ||  >>`).

## Build
`./gradlew publishToMavenLocal` in BBS-RE, then `./gradlew build` here (GitHub Actions does it and attaches the jar to a pre-release).
