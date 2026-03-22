# Plan: J2ME Direct-Boot Wrapper / Porting Template

## Goal
Convert the Jade (J2ME Loader) project into a **single-JAR direct-boot wrapper**.
- Bundle a `.jar` file in `assets/`
- On app launch, skip the app list and immediately run the bundled JAR
- Make this usable as a clean porting template for any J2ME game

---

## Background: Existing Infrastructure

The project **already has a `midlet` build flavor** in `app/build.gradle` with:
- A dedicated `AndroidManifest.xml` at `app/src/midlet/AndroidManifest.xml`
- Dedicated ProGuard rules in `proguard-midlet.pro`
- Source set `app/src/midlet/java/` (to be created/extended)

We build on top of the `midlet` flavor rather than modifying the main `open`/`play` flavor.

---

## Files to Change

### Phase 1 – Core Boot Logic (Critical)

1. **`app/src/main/java/ru/playsoftware/j2meloader/MainActivity.java`**
   - Currently: shows `AppsListFragment` (the app list)
   - Needed: In midlet flavor, detect bundled asset JAR and jump straight to emulator
   - Approach: Add a `midlet`-flavor override or a compile-time flag check

2. **`app/src/midlet/AndroidManifest.xml`**
   - Currently: 34 LOC, has custom activities
   - Needed: Set launcher activity to directly invoke the emulator activity
   - Remove intent filters for JAR/JAD file picking (not needed in wrapper mode)

3. **New file: `app/src/midlet/java/ru/playsoftware/j2meloader/MidletActivity.java`**
   - A lean Activity that:
     a. Copies the bundled `assets/game.jar` to internal storage on first run
     b. Installs it via `AppInstaller`
     c. Launches the emulator directly

4. **`app/src/main/assets/`** (template placeholder)
   - Add a `game.jar` placeholder (or document where to drop the JAR)
   - Optionally: a `game.jad` descriptor for metadata (name, vendor, version)

### Phase 2 – Codebase Cleanup (Redundancy Removal)

These don't affect the direct-boot feature but reduce APK size significantly:

5. **Unused vendor 3D APIs** (safe to remove from midlet flavor builds):
   - `com/jblend/graphics/j3d/` (~10 files, duplicate of motorola)
   - `com/motorola/graphics/j3d/` (~10 files, duplicate of jblend)
   - `com/vodafone/v10/graphics/j3d/` (~10 files, duplicate)
   - Keep: `javax/microedition/m3g/` (standard) + `com/mascotcapsule/micro3d/v3/` (most-used vendor)

6. **Dead vendor APIs:**
   - `com/nokia/mid/payment/` (6 files) – Nokia in-app purchase, unused
   - `com/motorola/funlight/` (2-3 files) – obscure LED API
   - `com/sonyericsson/accelerometer/` – general Android sensor API used instead

7. **ProGuard rules (`proguard-midlet.pro`)**:
   - Tighten rules to only keep packages actually used by the target JAR
   - Currently keeps all `javax/**` and `com/**` unconditionally

### Phase 3 – Template Polish

8. **`app/build.gradle`**
   - Ensure midlet flavor has `applicationIdSuffix` or unique `applicationId` so multiple wrapper APKs can coexist
   - Add `resValue` for app name derived from JAD metadata or a build property

9. **`app/src/main/res/values/strings.xml`** / midlet override
   - Allow the app name to be set per-game via a strings overlay in `app/src/midlet/res/`

10. **README / template instructions**
    - Document: "drop your JAR here, set app name here, build midlet flavor"

---

## What We Do NOT Touch

- `dexlib/` module — needed for bytecode transformation, leave as-is
- `javax/microedition/` core APIs — the J2ME emulation engine
- `com/mascotcapsule/micro3d/v3/` — active 3D implementation
- `app/src/main/cpp/m3g/` — native 3D engine
- All `play`/`open`/`fdroid` build flavors — unchanged

---

## Build Variants After This Plan

| Flavor | Behavior |
|--------|----------|
| `open` / `play` | Unchanged — full J2ME emulator with app list |
| `midlet` | **Direct-boot wrapper** — bundles one JAR, launches immediately |

---

## Implementation Order

1. Read `midlet` manifest + `MainActivity.java` to understand current state
2. Implement `MidletActivity.java` with asset copy + install + launch logic
3. Update `midlet` manifest to use new activity as launcher
4. Test boot flow with a sample JAR
5. Remove dead vendor packages (cleanup pass)
6. Tighten ProGuard for midlet flavor
7. Document template usage
