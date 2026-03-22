# Porting a J2ME Game — Quick Guide

This project includes a `midlet` build flavor that wraps a single J2ME JAR into a
standalone Android APK. No emulator UI, no app list — the game boots directly on launch.

---

## Quick Start

1. **Drop your JAR** into `app/src/main/assets/game.jar`
2. **Edit the manifest** at `app/src/midlet/resources/MIDLET-META-INF/MANIFEST.MF`
   — set `MIDlet-Name`, `MIDlet-Vendor`, `MIDlet-Version`, and the main class in `MIDlet-1`
3. **Tune the emulator config** at `app/src/midlet/assets/configs/midlet/config.json`
   — set screen resolution, orientation, keyboard layout, font sizes
4. **Build:** `./gradlew assembleMidletRelease`

The APK `applicationId` and launcher label are both derived from `MIDlet-Name` in step 2,
so set it before building. Spaces become underscores in the package name
(`My Game` → `com.example.androidlet.my_game`).

Replace the launcher icons in `app/src/main/res/mipmap-*/ic_launcher*` with your game's icon.

---

## Template Files Reference

| File | What it controls |
|---|---|
| `app/src/main/assets/game.jar` | The J2ME game to wrap — **replace this** |
| `app/src/midlet/resources/MIDLET-META-INF/MANIFEST.MF` | App name, version, main MIDlet class |
| `app/src/midlet/assets/configs/midlet/config.json` | Emulator settings (screen, keyboard, fonts) |
| `app/src/midlet/java/com/example/porting/HelloMidlet.java` | Sample MIDlet — replace or delete |
| `app/src/midlet/res/values/strings.xml` | Override help/about text shown in the game menu |

---

## config.json — Key Fields

| Field | Default | Notes |
|---|---|---|
| `ScreenWidth` / `ScreenHeight` | 240 / 320 | Original game canvas size in pixels |
| `Orientation` | 0 | 0 = portrait, 1 = landscape |
| `ScreenScaleToFit` | true | Fills the display while preserving aspect ratio |
| `GraphicsMode` | 1 | 1 = OpenGL, 0 = software |
| `ShowKeyboard` | true | Show virtual D-pad/keypad overlay |
| `VirtualKeyboardType` | 0 | 0 = standard grid, 1 = phone keypad |
| `VirtualKeyboardAlpha` | 64 | Keyboard overlay transparency (0–255) |
| `FontSizeSmall/Medium/Large` | 18/22/26 | Font sizes in px; increase for high-DPI screens |
| `ForceFullscreen` | false | Per-game fullscreen; global fullscreen is set separately |

---

## Global Preferences (set once on first launch)

`BootActivity` writes these defaults when the game installs for the first time:

| Preference | Default | Effect |
|---|---|---|
| `PREF_TOOLBAR` | false | Hides the action bar |
| `PREF_STATUSBAR` | false | Hides the status bar (fullscreen) |
| `PREF_KEEP_SCREEN` | true | Prevents screen sleep during gameplay |
| `PREF_VIBRATION` | true | Enables haptic feedback |

To change defaults, edit `applyGlobalPreferences()` in
`app/src/midlet/java/ru/playsoftware/j2meloader/BootActivity.java` before building.

---

## How the Boot Flow Works

On **first launch**: copies `assets/game.jar` to internal storage, converts it to DEX
bytecode, writes descriptor and config files, then starts the game.

On **subsequent launches**: checks that all runtime files exist, reads the game name from
the saved descriptor, and boots directly — no conversion.

On **clear app data**: runtime files are gone, so first-launch conversion runs again cleanly.

---

## Tips

- **Large JARs (>1 MB)** take a few seconds to convert on first launch. The spinner is
  shown during this — it's not frozen.
- **System properties** (`SystemProperties` in config.json) are populated automatically
  from `assets/defaults/system.props` on first install. Leave the field empty in the
  config file.
- **Multiple ports** can coexist on the same device — each gets a unique `applicationId`
  derived from its `MIDlet-Name`.
- To **block access to emulator settings**, `ConfigActivity` and `SettingsActivity` are
  replaced by stubs that immediately close. Users cannot change emulator settings at runtime.
