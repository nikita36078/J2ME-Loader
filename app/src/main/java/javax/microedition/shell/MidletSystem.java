package javax.microedition.shell;


import android.text.TextUtils;

import androidx.annotation.Keep;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.lcdui.Display;

/** {@link java.lang.System} Delegate for Midlet */
@Keep
public final class MidletSystem {

    private static final Map<String, String> PROPERTY = new HashMap<>();

    static void setProperty(String key, String value) {
        PROPERTY.put(key, value);
    }

    public static String getProperty(String key) {
        String value = PROPERTY.get(key);
        if (Display.isMultiTouchSupported() && key.equals("com.nokia.pointer.number")) {
            return Display.getDisplay(null).getPointerNumber();
        }
        if (TextUtils.isEmpty(value)) value = System.getProperty(key);
        return value;
    }

    public static String getProperty(String key, String def) {
        String value = PROPERTY.get(key);
        if (TextUtils.isEmpty(value)) value = System.getProperty(key, def);
        return value;
    }

}
