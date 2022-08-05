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

package ru.playsoftware.j2meloader.info;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import ru.playsoftware.j2meloader.R;

public class InfoDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity activity = requireActivity();
		TextView tv = new TextView(activity);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(Html.fromHtml(getString(R.string.about_message)));
		tv.setTextSize(16);
		float density = getResources().getDisplayMetrics().density;
		int paddingHorizontal = (int) (density * 20);
		int paddingVertical = (int) (density * 14);
		tv.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, 0);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.app_name)
				.setIcon(R.mipmap.ic_launcher)
				.setView(tv);
		return builder.create();
	}
}
