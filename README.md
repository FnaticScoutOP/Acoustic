# YTWrap — YouTube Media Wrapper for Android

A minimalist Android app that wraps YouTube as a background music player with Spotify-style media controls, local playlists, and liked tracks.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  MainActivity → MainScreen (Compose)                │
│  ├── SearchScreen (YouTube search via Extractor)    │
│  ├── LibraryScreen (Liked tracks + Playlists)       │
│  ├── MiniPlayer (collapsed bar)                     │
│  └── NowPlayingSheet (expanded bottom sheet)        │
├─────────────────────────────────────────────────────┤
│               ViewModel Layer                        │
│  PlayerViewModel ←→ MediaController ←→ Service      │
│  LibraryViewModel ←→ Repositories                   │
├─────────────────────────────────────────────────────┤
│               Service Layer                          │
│  PlaybackService (MediaSessionService + ExoPlayer)  │
│  └── Foreground service, notification, lock screen  │
├─────────────────────────────────────────────────────┤
│               Data Layer                             │
│  YouTubeExtractor (NewPipe Extractor → stream URLs) │
│  Room Database (tracks, playlists, cross-refs)      │
│  TrackRepository / PlaylistRepository               │
├─────────────────────────────────────────────────────┤
│                DI (Hilt)                             │
│  DatabaseModule / PlayerModule                      │
└─────────────────────────────────────────────────────┘
```

## Tech Stack

| Component         | Library                          |
|-------------------|----------------------------------|
| Language          | Kotlin 2.1                       |
| UI                | Jetpack Compose + Material 3     |
| Player Engine     | Media3 ExoPlayer 1.5.1           |
| Media Session     | Media3 Session (notification)    |
| Local DB          | Room 2.6.1                       |
| DI                | Hilt 2.54                        |
| Stream Extraction | NewPipe Extractor                |
| Image Loading     | Coil 2.7                         |
| HTTP Client       | OkHttp 4.12                      |

## Key Files

| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | Permissions (INTERNET, FOREGROUND_SERVICE, WAKE_LOCK, POST_NOTIFICATIONS), service declaration |
| `PlaybackService.kt` | MediaSessionService — owns ExoPlayer, handles background playback, custom commands |
| `YouTubeExtractor.kt` | NewPipe Extractor wrapper — resolves video IDs to audio stream URLs |
| `DownloaderImpl.kt` | OkHttp-backed HTTP downloader for NewPipe Extractor |
| `Track.kt` | Room entity — videoId, title, artist, thumbnail, duration, isLiked |
| `Playlist.kt` | Room entity — playlistId, name, description, timestamps |
| `PlaylistTrackCrossRef.kt` | Room join table — many-to-many between playlists and tracks |
| `PlayerViewModel.kt` | Connects to PlaybackService via MediaController, exposes playback state |
| `LibraryViewModel.kt` | Manages search, liked tracks, and playlists |
| `NowPlayingSheet.kt` | Spotify-style expanded player with seek bar, shuffle, repeat, like |
| `MainScreen.kt` | Root screen with bottom nav (Search / Library) + mini player |

## How Background Playback Works

1. `PlaybackService` extends `MediaSessionService` and runs as a **foreground service** with type `mediaPlayback`
2. ExoPlayer is configured with `AUDIO_CONTENT_TYPE_MUSIC` and `WAKE_MODE_NETWORK` to keep the CPU and network alive
3. `MediaSession` integration automatically generates a persistent notification with:
   - Play/Pause, Skip Next, Skip Previous buttons
   - Seek bar (on Android 13+)
   - Album art and track metadata
   - Lock screen controls
4. When the app is minimized, `onTaskRemoved()` only stops the service if nothing is playing
5. `PlayerViewModel` connects via `MediaController` — all commands go through the session

## How YouTube Stream Extraction Works

- **NewPipe Extractor** parses YouTube pages and extracts DASH audio-only streams
- `YouTubeExtractor.extractStream(videoId)` returns the best-bitrate audio URL + metadata
- `YouTubeExtractor.search(query)` performs YouTube search and returns `Track` objects
- No API key required — but this is a **legal gray area** (see note below)

### Legal Note

> NewPipe Extractor scrapes YouTube directly. This is the same approach used by NewPipe, LibreTube, and Piped.
> It does NOT use the YouTube Data API and does NOT require an API key.
> **Do NOT distribute on Google Play** without consulting legal counsel.
> This project is intended for **personal/educational use only**.

### Alternatives

- **Piped API** (`piped.video`): REST proxy wrapping NewPipe Extractor — avoids scraping from the device
- **Invidious API**: Another privacy-respecting YouTube proxy
- **YouTube Data API v3**: Official but does NOT provide stream URLs and prohibits background playback

## Building

1. Open the project in Android Studio
2. Sync Gradle
3. The NewPipe Extractor dependency in `build.gradle.kts` points to a JitPack artifact — update the tag to a real release:
   ```
   implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.5")
   ```
4. Build and run on a device/emulator (API 26+)

## Room Database Schema

```
tracks
├── video_id (PK)
├── title
├── artist
├── thumbnail_url
├── duration_ms
├── is_liked
└── added_at

playlists
├── playlist_id (PK, auto)
├── name
├── description
├── cover_url
├── created_at
└── updated_at

playlist_track_cross_ref
├── playlist_id (FK → playlists)
├── video_id (FK → tracks)
├── position
└── added_at
```

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
