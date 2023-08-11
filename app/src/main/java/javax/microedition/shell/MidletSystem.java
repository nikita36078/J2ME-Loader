package javax.microedition.shell;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.lcdui.Display;

/** {@link java.lang.System} Delegate for Midlet */
@Keep
public final class MidletSystem {
    private static final String TAG = MidletSystem.class.getName();
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

        Log.d(TAG, "System.getProperty: " + key + "=" + value);
        return value;
    }
}
