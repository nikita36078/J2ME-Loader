package ru.playsoftware.j2meloader.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Stub — global settings are not accessible in wrapper/porting mode.
 * Replaces the real SettingsActivity for the midlet build flavor.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}
