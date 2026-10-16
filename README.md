# Backdrop (test builds)

Addon for BBS FS 2.7: video **with sound** behind BBS, and **Spotify** in it.
Addon cho BBS FS 2.7: video có tiếng làm nền, kèm Spotify.

## Works on
**BBS FS 2.7** (Fabric 1.20.4), through its addon API: a **backdrop** category in BBS's settings screen, a **Backdrop** button on the landing-screen banner (opens the panel),
its own language files (English, Tiếng Việt). Two mixins draw the wallpaper (`UIDashboard.background`, film editor included) and the banner
(`UILandingScreen.renderBanner`, where it exists). Not for mchorse's original BBS.

## Use
1. Put the jar in `mods/` (needs Fabric API and ffmpeg: BBS's own ffmpeg path is reused). Videos (`.mp4`, H.264, ≤720p) go in BBS `assets/video/`.
2. Press the **play button** on the banner of the landing screen, pick a video for the wallpaper and/or banner, set volume and sound offset.

## Spotify
Spotify cannot be played by a mod; this is a remote for the app you already have open. Reading works for any account, controls need Premium.
1. developer.spotify.com/dashboard → Create app, Redirect URI `http://127.0.0.1:8977/callback`, "Web API".
2. In the Backdrop panel : paste the Client ID, sign in.
3. Start a song in Spotify: a card appears in the corner of BBS screens (`<<  ||  >>`).

## Build
`./gradlew publishToMavenLocal` in BBS-RE, then `./gradlew build` here (GitHub Actions does it and attaches the jar to a pre-release).
