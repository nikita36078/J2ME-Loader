/*
 * Copyright 2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader.base;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import ru.playsoftware.j2meloader.R;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

	private ColorStateList btnColorStateList = null;
	private int cardBgColor;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String theme = preferences.getString("pref_theme", "light");
		if (theme.equals("dark")) {
			setTheme(R.style.AppTheme);
		} else {
			setTheme(R.style.AppTheme_Light);
		}
		if (getSupportActionBar() != null) {
			getSupportActionBar().setElevation(getResources().getDisplayMetrics().density * 2);
		}
		super.onCreate(savedInstanceState);
	}

	protected void setControlColorTint(ViewGroup viewGroup) {
		if (btnColorStateList == null) {
			//button
			int btnColorNormal = getThemeAttributeColorInt(R.attr.buttonBgColorNormal);
			int btnColorPressed = getThemeAttributeColorInt(R.attr.buttonBgColorPressed);
			btnColorStateList =
					new ColorStateList(new int[][]{new int[]{android.R.attr.state_pressed}, new int[]{}},
							new int[]{btnColorPressed, btnColorNormal});
			//card
			cardBgColor = getThemeAttributeColorInt(R.attr.configCardBgColor);
		}
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View child = viewGroup.getChildAt(i);
			Drawable background = child.getBackground();
			if (child.getTag() != null && background != null) {
				if (child.getTag().equals(getString(R.string.tag_theme_button))) {
					//set button background
					Drawable wrap = DrawableCompat.wrap(background);
					DrawableCompat.setTintList(wrap, btnColorStateList);
					child.setBackgroundDrawable(wrap);
				} else if (child.getTag().equals(getString(R.string.tag_theme_card))) {
					//set card background
					DrawableCompat.setTint(DrawableCompat.wrap(child.getBackground()),
							cardBgColor);
				}
			}
			if (child instanceof ViewGroup) {
				setControlColorTint((ViewGroup) child);
			}
		}
	}

	private int getThemeAttributeColorInt(int attr) {
		TypedValue outValue = new TypedValue();
		//default color
		int color = ContextCompat.getColor(this, R.color.light_color_primary);
		if (getTheme().resolveAttribute(attr, outValue, false)) {
			if (outValue.type == TypedValue.TYPE_REFERENCE)
				color = ContextCompat.getColor(this, outValue.data);
			else
				color = outValue.data;
		}
		return color;
	}
}
