/*
 * Copyright 2024 J2ME Loader Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.playsoftware.j2meloader;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.dx.command.dexer.Main;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ProfileModel;
import ru.playsoftware.j2meloader.config.ProfilesManager;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.woesss.j2me.jar.Descriptor;

import static ru.playsoftware.j2meloader.util.Constants.PREF_ADD_CUTOUT_AREA;
import static ru.playsoftware.j2meloader.util.Constants.PREF_KEEP_SCREEN;
import static ru.playsoftware.j2meloader.util.Constants.PREF_SCREENSHOT_SWITCH;
import static ru.playsoftware.j2meloader.util.Constants.PREF_STATUSBAR;
import static ru.playsoftware.j2meloader.util.Constants.PREF_TOOLBAR;
import static ru.playsoftware.j2meloader.util.Constants.PREF_VIBRATION;

/**
 * Entry point for the J2ME wrapper template.
 *
 * On first launch: converts assets/game.jar (+ optional assets/game.jad) into the
 * required runtime files and writes the bundled config.json for the emulator.
 *
 * On subsequent launches: verifies all runtime files are present and boots directly
 * into the game without any conversion step.
 *
 * Settings and Config activities are replaced by stubs — the end user cannot access them.
 */
public class BootActivity extends AppCompatActivity {

    /** Fixed app directory name — matches the path hardcoded in MicroActivity (FULL_EMULATOR=false). */
    private static final String APP_DIR_NAME = "midlet";

    private static final String ASSET_JAR  = "game.jar";
    private static final String ASSET_JAD  = "game.jad";
    private static final String ASSET_CONFIG = "configs/midlet/config.json";

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boot);

        if (allRuntimeFilesExist()) {
            launchGame();
        } else {
            installAndLaunch();
        }
    }

    // -------------------------------------------------------------------------
    // Runtime file check
    // -------------------------------------------------------------------------

    /**
     * Returns true only when every file that MicroLoader requires is present.
     * config.json is mandatory — MicroLoader.init() returns false if absent,
     * which would re-trigger ConfigActivity (now a no-op stub, causing a silent loop).
     */
    private boolean allRuntimeFilesExist() {
        File base      = new File(Config.getAppDir(), APP_DIR_NAME);
        File configDir = new File(Config.getConfigsDir(), APP_DIR_NAME);
        return new File(base,      Config.MIDLET_DEX_FILE.substring(1)).exists()      // converted.dex
            && new File(base,      Config.MIDLET_MANIFEST_FILE.substring(1)).exists() // converted.dex.conf
            && new File(base,      Config.MIDLET_RES_FILE.substring(1)).exists()      // res.jar
            && new File(configDir, Config.MIDLET_CONFIG_FILE.substring(1)).exists();  // config.json
    }

    // -------------------------------------------------------------------------
    // First-run install
    // -------------------------------------------------------------------------

    private void installAndLaunch() {
        disposables.add(
            Single.<String>create(emitter -> {
                try {
                    String name = performInstall();
                    emitter.onSuccess(name);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onInstallSuccess, this::onInstallError)
        );
    }

    /**
     * Runs on a background thread.
     * Converts game.jar → runtime files and writes config.json.
     * @return the MIDlet display name read from the descriptor
     */
    private String performInstall() throws Exception {
        File cacheDir  = new File(getCacheDir(), "boot_install");
        cacheDir.mkdirs();

        // 1. Copy game.jar from assets to cache
        File cachedJar = new File(cacheDir, ASSET_JAR);
        copyAssetToFile(ASSET_JAR, cachedJar);

        // 2. Copy game.jad from assets to cache (optional)
        File cachedJad = null;
        if (assetExists(ASSET_JAD)) {
            cachedJad = new File(cacheDir, ASSET_JAD);
            copyAssetToFile(ASSET_JAD, cachedJad);
        }

        // 3. Ensure target directory exists: getFilesDir()/converted/midlet/
        File targetDir = new File(Config.getAppDir(), APP_DIR_NAME);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Cannot create app dir: " + targetDir);
        }

        // 4. DEX conversion: game.jar → converted.dex
        File dexFile = new File(targetDir, Config.MIDLET_DEX_FILE.substring(1));
        Main.main(new String[]{
            "--no-optimize",
            "--core-library",
            "--output=" + dexFile.getAbsolutePath(),
            cachedJar.getAbsolutePath()
        });

        // 5. Copy res.jar (original JAR kept for resource loading at runtime)
        File resJar = new File(targetDir, Config.MIDLET_RES_FILE.substring(1));
        FileUtils.copyFileUsingChannel(cachedJar, resJar);

        // 6. Parse descriptor: prefer JAD (richer metadata), fall back to JAR manifest
        Descriptor desc;
        if (cachedJad != null) {
            desc = new Descriptor(cachedJad, true);
        } else {
            desc = loadManifestFromJar(cachedJar);
        }

        // 7. Write converted.dex.conf (merged descriptor)
        File confFile = new File(targetDir, Config.MIDLET_MANIFEST_FILE.substring(1));
        desc.writeTo(confFile);

        // 8. Extract icon (best-effort — skip if absent or unreadable)
        try {
            String iconEntry = desc.getIcon();
            if (iconEntry != null) {
                File iconFile = new File(targetDir, Config.MIDLET_ICON_FILE.substring(1));
                ZipFile zip = new ZipFile(resJar);
                FileHeader fh = zip.getFileHeader(iconEntry);
                if (fh != null) {
                    ru.playsoftware.j2meloader.util.ZipUtils.unzipEntry(resJar, iconEntry, iconFile);
                }
            }
        } catch (Exception ignored) { /* icon is optional */ }

        // 9. Write global SharedPreferences (fullscreen, keep-screen-on, etc.)
        applyGlobalPreferences();

        // 10. Write config.json from bundled asset → getFilesDir()/configs/midlet/config.json
        File configDir = new File(Config.getConfigsDir(), APP_DIR_NAME);
        configDir.mkdirs();
        File configFile = new File(configDir, Config.MIDLET_CONFIG_FILE.substring(1));
        if (assetExists(ASSET_CONFIG)) {
            copyAssetToFile(ASSET_CONFIG, configFile);
            // Ensure systemProperties is populated by running through ProfilesManager migration
            ProfileModel params = ProfilesManager.loadConfig(configDir);
            if (params != null && (params.systemProperties == null || params.systemProperties.isEmpty())) {
                ProfilesManager.updateSystemProperties(params);
                ProfilesManager.saveConfig(params);
            }
        } else {
            // Fallback: generate defaults programmatically
            ProfileModel defaults = new ProfileModel(configDir);
            ProfilesManager.saveConfig(defaults);
        }

        // 11. Clean up cache
        FileUtils.deleteDirectory(cacheDir);

        return desc.getName();
    }

    private void onInstallSuccess(String midletName) {
        Config.startApp(this, midletName, Config.getAppDir() + APP_DIR_NAME, false);
        finish();
    }

    private void onInstallError(Throwable e) {
        Toast.makeText(this, "Failed to load game: " + e.getMessage(), Toast.LENGTH_LONG).show();
        finish();
    }

    // -------------------------------------------------------------------------
    // Direct launch (subsequent runs)
    // -------------------------------------------------------------------------

    private void launchGame() {
        try {
            File confFile = new File(Config.getAppDir() + APP_DIR_NAME,
                    Config.MIDLET_MANIFEST_FILE.substring(1));
            Descriptor desc = new Descriptor(confFile, false);
            Config.startApp(this, desc.getName(), Config.getAppDir() + APP_DIR_NAME, false);
        } catch (Exception e) {
            // Descriptor unreadable — force re-install next time by deleting converted.dex
            new File(Config.getAppDir() + APP_DIR_NAME,
                    Config.MIDLET_DEX_FILE.substring(1)).delete();
            Toast.makeText(this, "Reinstalling game…", Toast.LENGTH_SHORT).show();
            installAndLaunch();
            return;
        }
        finish();
    }

    // -------------------------------------------------------------------------
    // Global SharedPreferences (locked defaults for wrapper mode)
    // -------------------------------------------------------------------------

    private void applyGlobalPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean(PREF_TOOLBAR,          false) // no toolbar — full game view
            .putBoolean(PREF_STATUSBAR,        false) // no status bar — fullscreen
            .putBoolean(PREF_KEEP_SCREEN,      true)  // keep screen on during gameplay
            .putBoolean(PREF_VIBRATION,        true)  // haptic feedback on
            .putBoolean(PREF_ADD_CUTOUT_AREA,  false) // standard safe area
            .putBoolean(PREF_SCREENSHOT_SWITCH,false)
            .apply();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean assetExists(String assetPath) {
        try (InputStream is = getAssets().open(assetPath)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void copyAssetToFile(String assetPath, File dest) throws IOException {
        try (InputStream in = getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    /**
     * Reads META-INF/MANIFEST.MF from a JAR and returns a Descriptor.
     * Used when no .jad file is bundled.
     */
    private Descriptor loadManifestFromJar(File jar) throws IOException {
        net.lingala.zip4j.ZipFile zip = new net.lingala.zip4j.ZipFile(jar);
        net.lingala.zip4j.model.FileHeader mf = zip.getFileHeader(JarFile.MANIFEST_NAME);
        if (mf == null) throw new IOException("JAR has no " + JarFile.MANIFEST_NAME);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read;
        try (InputStream is = zip.getInputStream(mf)) {
            while ((read = is.read(buf)) != -1) baos.write(buf, 0, read);
        }
        return new Descriptor(baos.toString(), false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
