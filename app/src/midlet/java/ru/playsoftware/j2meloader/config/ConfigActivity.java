package ru.playsoftware.j2meloader.config;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Stub — settings are not accessible in wrapper/porting mode.
 * Replaces the real ConfigActivity for the midlet build flavor.
 * Any intent that would open settings (e.g. from MicroActivity's back-press dialog)
 * is silently dismissed by immediately finishing this activity.
 */
public class ConfigActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
