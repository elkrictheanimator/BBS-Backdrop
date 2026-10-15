# Backdrop (test builds)

Video **with sound** behind BBS, and **Spotify** in it. / Video có tiếng làm nền cho BBS, kèm Spotify.

Works as a plain Fabric mod (1.20.4) — no BBS addon API needed — so it targets BBS 2.0+, BBS FS 2.5.2–2.7 and BBS-RE.
Hạn chế: chưa thử trên máy thật, hãy báo lỗi kèm log.

## Use / Cách dùng

1. Put the jar in `mods/` (needs Fabric API and ffmpeg; BBS's ffmpeg path is reused, or `/backdrop ffmpeg <path>`).
2. Put videos (`.mp4`, H.264, ≤720p) in BBS `assets/video/`.
3. In game, chat commands (everything is saved in `config/backdrop.json`):

```
/backdrop videos
/backdrop wallpaper video <file> | url <mp4 link> | on | off | clear | volume 0-1 | sync <ms>
/backdrop banner    video <file> | url <mp4 link> | on | off | clear | volume 0-1 | sync <ms>
/backdrop darkness 0-1      (veil over the wallpaper)
/backdrop panels 0-1        (panel opacity, BBS-RE only)
```

The **wallpaper** shows behind every editor, the film editor included. The **banner** replaces the landing-screen banner (only where that screen has one).
`sync`: positive when the sound comes before the picture, negative when after.

## Spotify

Spotify cannot be played by a mod; this is a remote for the app you already have open (phone / desktop). Reading works for any account, controls need Premium.

1. Go to https://developer.spotify.com/dashboard → Create app. Redirect URI: `http://127.0.0.1:8977/callback`. Check "Web API".
2. `/backdrop spotify id <Client ID>` then `/backdrop spotify login` (browser opens).
3. Start a song in Spotify. A card appears in the corner of BBS screens (`<<  ||  >>`).
4. `/backdrop spotify play|pause|next|prev|volume <0-100>|card true|false|logout`

## Build

`./gradlew publishToMavenLocal` in BBS-RE, then `./gradlew build` here (GitHub Actions does it and attaches the jar to a pre-release).
