package ru.playsoftware.j2meloader.base;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ru.playsoftware.j2meloader.R;

public class BaseActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(
				this,
				SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primary)),
				SystemBarStyle.dark(Color.BLACK)
		);
		View view = findViewById(android.R.id.content);
		ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
			v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
			return WindowInsetsCompat.CONSUMED;
		});
	}
}
