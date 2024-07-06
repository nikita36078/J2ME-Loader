package ru.playsoftware.j2meloader.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SAFDirResultContract extends ActivityResultContract<String, Uri> {
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, String s) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return i;
    }

    @Override
    public Uri parseResult(int i, @Nullable Intent intent) {
        if (i == Activity.RESULT_OK && intent != null) {
            return intent.getData();
        }
        return null;
    }
}
