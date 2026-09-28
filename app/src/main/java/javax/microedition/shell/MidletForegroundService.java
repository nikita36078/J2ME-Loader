/*
 * Copyright 2024 Arman Jussupgaliyev
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

package javax.microedition.shell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.BuildConfig;
import ru.playsoftware.j2meloader.R;

import static ru.playsoftware.j2meloader.util.Constants.PREF_KEEP_SCREEN;

/**
 * Foreground service to keep MIDlet running in background on modern Android devices.
 * Prevents process killing and maintains network connectivity during Doze mode.
 */
public class MidletForegroundService extends Service {
    private static final String TAG = "MidletFGService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "midlet_foreground_channel";

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private static MidletForegroundService instance;

    public static final String ACTION_START = "ru.playsoftware.j2meloader.ACTION_START_FOREGROUND";
    public static final String ACTION_STOP = "ru.playsoftware.j2meloader.ACTION_STOP_FOREGROUND";
    public static final String EXTRA_APP_NAME = "app_name";

    public static boolean isRunning() {
        return instance != null;
    }

    public static void start(Context context, String appName) {
        if (instance != null) {
            Log.d(TAG, "Service already running");
            return;
        }
        Intent intent = new Intent(context, MidletForegroundService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_APP_NAME, appName);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            // Do not crash the game startup if the system refuses to start the
            // foreground service (e.g. some OEM battery managers).
            Log.e(TAG, "Failed to start foreground service", e);
        }
    }

    public static void stop(Context context) {
        if (instance == null) {
            Log.d(TAG, "Service not running");
            return;
        }
        Intent intent = new Intent(context, MidletForegroundService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        String appName = intent != null ? intent.getStringExtra(EXTRA_APP_NAME) : "MIDlet";
        try {
            Notification notification = createNotification(appName);
            startForeground(NOTIFICATION_ID, notification);
            acquireLocks();
            Log.d(TAG, "Service started for: " + appName);
            return START_STICKY;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service", e);
            instance = null;
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        releaseLocks();
        instance = null;
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_midlet),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_midlet_desc));
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String appName) {
        Intent notificationIntent = new Intent(this, MicroActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(appName)
            .setContentText(getString(R.string.notification_midlet_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }

    private void acquireLocks() {
        // Acquire partial wake lock to keep CPU running
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "J2ME-Loader::MidletWakeLock"
            );
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
            Log.d(TAG, "WakeLock acquired");
        }

        // Acquire WiFi lock to maintain network during Doze
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null) {
            try {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "J2ME-Loader::MidletWifiLock");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
                Log.d(TAG, "WiFi lock acquired");
            } catch (Exception e) {
                Log.e(TAG, "Failed to acquire WiFi lock", e);
            }
        }
    }

    private void releaseLocks() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
            Log.d(TAG, "WakeLock released");
        }

        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            wifiLock = null;
            Log.d(TAG, "WiFi lock released");
        }
    }
}
