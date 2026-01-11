# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TVRemoteIME (小盒精灵) is an Android TV box management application that provides:
- Cross-screen remote text input via custom Input Method Editor (IME)
- Remote control functionality through web browser interface
- File management, app management, video playback (HTTP/RTMP/MMS/torrent/ED2K)
- DLNA video casting support

The app runs an embedded HTTP server (NanoHTTPD on port 9978) that serves a web control interface.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Build specific module
./gradlew :IMEService:assembleDebug
```

Release APK output: `IMEService/build/outputs/apk/release/IMEService-release.apk`

## Project Architecture

### Module Structure

```
TVRemoteIME/
├── IMEService/          # Main application (com.android.tvremoteime)
├── AdbLib/              # Pure Java ADB protocol library
├── DroidDLNA/           # DLNA/UPnP media renderer (Cling-based)
├── ijkplayer/           # Video player (Bilibili ijkplayer wrapper)
└── thunder/             # Xunlei download SDK for torrent/ED2K
```

### IMEService Core Components

**Entry Points:**
- `IMEService.java` - Android InputMethodService, starts HTTP server on creation
- `MainActivity.java` - Launcher activity for manual service start

**HTTP Server (`server/` package):**
- `RemoteServer.java` - NanoHTTPD server, routes requests to processors
- `InputRequestProcesser.java` - Handles `/text`, `/key`, `/keydown`, `/keyup` endpoints
- `FileRequestProcesser.java` - File browsing and management
- `AppRequestProcesser.java` - App install/uninstall/launch
- `PlayRequestProcesser.java` - Video playback control
- `RawRequestProcesser.java` - Serves static web resources from `res/raw/`

**Key Services:**
- `AdbHelper.java` - Falls back to ADB mode when not set as default IME
- `DLNAUtils.java` - DLNA service management
- `AutoUpdateManager.java` - Self-update functionality

### Web Interface

Static files served from Android raw resources (`R.raw.*`):
- `index.html` - Main control page
- `style.css` - Styles
- `ime_core.js` - JavaScript control logic
- `jquery_min.js` - jQuery library

### Data Flow

1. Web browser sends HTTP POST to `/key` with keycode
2. `RemoteServer.serve()` routes to `InputRequestProcesser`
3. `DataReceiver.onKeyEventReceived()` callback in `IMEService`
4. Key event sent via `InputConnection` or falls back to ADB

## Important Technical Notes

- **Target Architecture:** armeabi-v7a only (32-bit ARM native libraries)
- **HTTP Server Port:** 9978 (increments if occupied, up to 9999)
- **Input Method:** Must be set as default IME for full functionality; otherwise uses ADB fallback
- **Native Libraries:** Located in `ijkplayer/libs/` and `thunder/libs/`

## Legacy Codebase Warning

This project uses severely outdated dependencies (2018 era):
- Gradle 4.4, Android Gradle Plugin 3.1.0
- targetSdkVersion 15, compileSdkVersion 25-26
- Uses deprecated `compile` instead of `implementation`
- Uses `jcenter()` (now defunct)
- Uses Android Support Library instead of AndroidX

Modernization requires migrating to AndroidX, updating Gradle/AGP, and raising SDK versions.
