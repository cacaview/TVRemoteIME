# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TVRemoteIME (小盒精灵) is an Android TV box management application that provides:
- Cross-screen remote text input via custom Input Method Editor (IME)
- Remote control functionality through web browser interface
- **Touchpad/mouse control via AccessibilityService** (NEW in v2.0.0)
- File management, app management, video playback (HTTP/RTMP/MMS/torrent/ED2K)
- DLNA video casting support

The app runs an embedded HTTP server (NanoHTTPD on port 9978) that serves a web control interface.

## Current Version

- **versionCode**: 12
- **versionName**: 2.0.0

## Build Commands

```bash
# Build debug APK (signed, can be installed directly)
./gradlew assembleDebug

# Build release APK (unsigned, requires signing before installation)
./gradlew assembleRelease

# Clean build
./gradlew clean

# Build specific module
./gradlew :IMEService:assembleDebug
```

Release APK output: `IMEService/build/outputs/apk/release/IMEService-release.apk`

## Modernization Status (2026-01 Update)

✅ **Completed Modernization:**
- Gradle upgraded to 8.13
- Android Gradle Plugin upgraded to 8.13.2
- compileSdkVersion upgraded to 36
- targetSdkVersion upgraded to 36
- minSdkVersion: 21 (Android 5.0+)
- Java compatibility: VERSION_17
- AndroidX migration completed (all modules use androidx.*)
- Namespace declarations added to all modules
- Fixed Android 5.0+ explicit Intent requirements
- Fixed ProGuard configuration compatibility
- All dependencies updated to use `implementation` instead of deprecated `compile`

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
- **`MouseRequestProcesser.java`** - NEW: Handles touchpad/mouse control endpoints

**Touchpad/Mouse Control (`mouse/` package):**
- `MouseAccessibilityService.java` - AccessibilityService for gesture simulation
- `MouseCursorOverlay.java` - Floating cursor overlay implementation

**Key Services:**
- `AdbHelper.java` - Falls back to ADB mode when not set as default IME
- `DLNAUtils.java` - DLNA service management
- `AutoUpdateManager.java` - Self-update functionality

### Touchpad/Mouse API Endpoints

| Endpoint | Method | Parameters | Description |
|----------|--------|------------|-------------|
| `/mouse/status` | GET | - | Get service status and mouse position |
| `/mouse/move` | POST | dx, dy | Move mouse (relative movement) |
| `/mouse/click` | POST | button (0=left, 1=right) | Mouse click |
| `/mouse/scroll` | POST | dy | Scroll wheel (positive=down, negative=up) |
| `/mouse/show` | POST | - | Show cursor |
| `/mouse/hide` | POST | - | Hide cursor |

**Requirements:**
- Android 7.0+ (API 24+) for gesture dispatch
- AccessibilityService must be enabled in system settings
- Uses `TYPE_ACCESSIBILITY_OVERLAY` for cursor display (Android 8.0+)

### Web Interface

Static files served from Android raw resources (`R.raw.*`):
- `index.html` - Main control page
- `style.css` - Styles
- `ime_core.js` - JavaScript control logic
- `jquery_min.js` - jQuery library

### Data Flow

**Keyboard/Text Input:**
1. Web browser sends HTTP POST to `/key` with keycode
2. `RemoteServer.serve()` routes to `InputRequestProcesser`
3. `DataReceiver.onKeyEventReceived()` callback in `IMEService`
4. Key event sent via `InputConnection` or falls back to ADB

**Mouse/Touchpad Input:**
1. Web browser sends HTTP POST to `/mouse/move`, `/mouse/click`, etc.
2. `RemoteServer.serve()` routes to `MouseRequestProcesser`
3. `MouseAccessibilityService.dispatchGesture()` for gesture simulation
4. Cursor position updated via `MouseCursorOverlay`

## Important Technical Notes

- **Target Architecture:** armeabi-v7a only (32-bit ARM native libraries)
- **HTTP Server Port:** 9978 (increments if occupied, up to 9999)
- **Input Method:** Must be set as default IME for full functionality; otherwise uses ADB fallback
- **Native Libraries:** Located in `ijkplayer/libs/` and `thunder/libs/`
- **AccessibilityService:** Required for touchpad/mouse control; enable in Settings → Accessibility

## AndroidX Dependencies

All modules have been migrated to AndroidX:
- `androidx.annotation:annotation:1.7.1`
- `androidx.appcompat:appcompat:1.6.1`
- `androidx.recyclerview:recyclerview:1.3.2`
- `androidx.cardview:cardview:1.0.0`
- `com.google.android.material:material:1.11.0`

## Build System Details

### Gradle Configuration
```gradle
plugins {
    id 'com.android.application' version '8.13.2'
    id 'com.android.library' version '8.13.2'
}
```

### Module Configuration (All modules)
```gradle
android {
    namespace 'com.android.tvremoteime'  // or appropriate namespace
    compileSdk 36
    buildToolsVersion "36.1.0"

    defaultConfig {
        minSdk 21
        targetSdk 36
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}
```