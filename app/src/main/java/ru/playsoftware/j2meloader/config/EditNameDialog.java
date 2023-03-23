/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.playsoftware.j2meloader.config;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.databinding.DialogChangeNameBinding;

public class EditNameDialog extends DialogFragment {

	private static final String TITLE = "title";
	private static final String ID = "id";
	private static final String OLD_NAME = "oldName";
	private Callback callback;
	private String mTitle;
	private int mId;
	private String mOldName;

	private DialogChangeNameBinding binding;

	static EditNameDialog newInstance(String title, int id, String oldName) {
		EditNameDialog fragment = new EditNameDialog();
		Bundle args = new Bundle();
		args.putString(TITLE, title);
		args.putInt(ID, id);
		args.putString(OLD_NAME, oldName);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof Callback) {
			callback = ((Callback) context);
		}
		final Bundle args = getArguments();
		if (args != null) {
			mTitle = args.getString(TITLE);
			mId = args.getInt(ID);
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		binding = DialogChangeNameBinding.inflate(LayoutInflater.from(getContext()));

		AlertDialog dialog = new AlertDialog.Builder(requireActivity())
				.setTitle(mTitle).setView(binding.getRoot()).create();
		if (!TextUtils.isEmpty(mOldName)) {
			binding.editText.setText(mOldName);
			binding.editText.setSelection(mOldName.length());
		}
		binding.negativeButton.setOnClickListener(v1 -> dismiss());
		binding.positiveButton.setOnClickListener(v1 -> onClickOk(binding.editText));
		return dialog;
	}

	private void onClickOk(EditText editText) {
		String name = editText.getText().toString().trim().replaceAll("[/\\\\:*?\"<>|]", "");
		if (name.isEmpty()) {
			editText.setText(name);
			editText.requestFocus();
			editText.setSelection(name.length());
			Toast.makeText(requireActivity(), R.string.error_name, Toast.LENGTH_SHORT).show();
			return;
		}
		final File config = new File(Config.getProfilesDir(), name + Config.MIDLET_CONFIG_FILE);
		if (config.exists()) {
			editText.setText(name);
			editText.requestFocus();
			editText.setSelection(name.length());
			final Toast toast = Toast.makeText(requireActivity(), R.string.not_saved_exists, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 50);
			toast.show();
			return;
		}
		if (callback != null) {
			callback.onNameChanged(mId, name);
		}
		dismiss();
	}

	interface Callback {
		void onNameChanged(int id, String newName);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
