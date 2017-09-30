/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ua.naiksoftware.j2meloader;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		StringBuilder message = new StringBuilder().append(getText(R.string.about_message))
				.append(getText(R.string.version))
				.append(BuildConfig.VERSION_NAME)
				.append(getText(R.string.about_email))
				.append(getText(R.string.about_github))
				.append(getText(R.string.about_4pda))
				.append(getText(R.string.about_copyright));
		TextView tv = new TextView(getActivity());
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(Html.fromHtml(message.toString()));
		tv.setTextSize(16);
		tv.setPadding(10, 10, 10, 10);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.app_name)
				.setIcon(R.mipmap.ic_launcher)
				.setView(tv);
		return builder.create();
	}
}
