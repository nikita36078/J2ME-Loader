package filelog;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Log {

    private static final String token = " : ";
    private static final long MAX_LEN = 300 * 1024;//50 Kb

    public static void d(String tag, String message) {
        try {
            boolean noClear;
            File file = new File(Environment.getExternalStorageDirectory(), "log_j2meloader.txt");
            if (file.length() > MAX_LEN) {
                noClear = false;
            } else {
                noClear = true;
            }
            FileWriter fw = new FileWriter(file, noClear);
            String msg = "\n" + new Date().toLocaleString() + token + tag + token + message;
            fw.write(msg);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            android.util.Log.e("L", "err in logging", e);
        }
    }
}
