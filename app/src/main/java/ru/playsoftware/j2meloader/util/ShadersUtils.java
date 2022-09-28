package ru.playsoftware.j2meloader.util;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.config.Config;

public final class ShadersUtils {

    private static final String ASSETS_SHADERS_PATH = "shaders/ext_shaders";


    public static void exportAssetsShaders() {
        Observable.<Boolean>create(emitter -> {
            synchronized (ASSETS_SHADERS_PATH) {
                FileUtils.copyFileFromAssets(ASSETS_SHADERS_PATH, new File(Config.getShadersDir()), true);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io()).subscribe();
    }

}
