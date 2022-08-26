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

import android.app.Dialog;
import android.os.Bundle;
import android.webkit.WebView;

import javax.microedition.util.ContextHolder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import ru.playsoftware.j2meloader.R;

public class LicensesDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String string = ContextHolder.getAssetAsString("licenses.html");
		CharSequence message = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY);
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setTitle(R.string.licenses)
				.setIcon(R.mipmap.ic_launcher)
				.setMessage(message);
		return builder.create();
	}
}
