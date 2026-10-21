/*
 *  Copyright 2020-2023 Yury Kharchenko
 *  Copyright 2026 Nikita Shakarun
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

package ru.woesss.j2me.installer;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.applist.AppListModel;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.databinding.DialogInstallerBinding;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.woesss.j2me.jar.Descriptor;

public class BatchInstallerDialog extends DialogFragment {
	private static final String ARG_URIS = "BatchInstallerDialog.uris";
	private static final int MAX_FAILED_LIST_SIZE = 10;

	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	private final List<Uri> uriQueue = new ArrayList<>();
	private final List<String> failedNames = new ArrayList<>();
	private final Map<Uri, String> uriNames = new HashMap<>();
	private boolean started;
	private boolean cancelled;
	private boolean finished;
	private boolean jarAttempted;
	private int total;
	private int processed;
	private int installed;
	private String currentSourceName;

	private AppRepository appRepository;
	private Button btnClose;
	private AppInstaller installer;
	private AlertDialog mDialog;

	private DialogInstallerBinding binding;

	public static BatchInstallerDialog newInstance(List<Uri> uris) {
		BatchInstallerDialog fragment = new BatchInstallerDialog();
		Bundle args = new Bundle();
		args.putParcelableArrayList(ARG_URIS, new ArrayList<>(uris));
		fragment.setArguments(args);
		fragment.setCancelable(false);
		return fragment;
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		AppListModel appListModel = new ViewModelProvider(requireActivity()).get(AppListModel.class);
		appRepository = appListModel.getAppRepository();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			dismissAllowingStateLoss();
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		binding = DialogInstallerBinding.inflate(LayoutInflater.from(getContext()));
		mDialog = new AlertDialog.Builder(requireActivity(), getTheme())
				.setIcon(R.mipmap.ic_launcher)
				.setView(binding.getRoot())
				.setTitle("MIDlet installer")
				.setMessage("")
				.setCancelable(false)
				.setNegativeButton(android.R.string.cancel, null)
				.create();
		return mDialog;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@Override
	public void onDestroy() {
		compositeDisposable.dispose();
		super.onDestroy();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (started) {
			return;
		}
		started = true;
		btnClose = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		btnClose.setOnClickListener(v -> {
			if (finished) {
				dismiss();
			} else {
				cancel();
			}
		});
		ArrayList<Uri> uris = requireArguments().getParcelableArrayList(ARG_URIS);
		if (uris == null || uris.isEmpty()) {
			dismissAllowingStateLoss();
			return;
		}
		total = uris.size();
		uriQueue.addAll(uris);

		binding.installationStatus.setText(R.string.loading_info);
		showProgress();
		Context context = requireContext();
		for (Uri uri : uris) {
			uriNames.put(uri, FileUtils.getDisplayName(context, uri));
		}
		nextFile();
	}

	private void nextFile() {
		if (cancelled) {
			return;
		}
		if (uriQueue.isEmpty()) {
			onFinished();
			return;
		}
		if (installer != null) {
			installer.clearCache();
		}
		Uri uri = uriQueue.remove(0);
		processed++;
		jarAttempted = false;
		currentSourceName = uriNames.get(uri);
		binding.installationStatus.setText(getString(R.string.install_multiple_progress,
				processed, total));
		mDialog.setMessage(currentSourceName);
		showProgress();
		installer = new AppInstaller(null, uri, requireActivity().getApplication(), appRepository);
		Disposable disposable = Single.create(installer::loadInfo)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private void onProgress(@NonNull Integer status) {
		if (!isAdded() || cancelled) {
			return;
		}
		Descriptor nd = installer.getNewDescriptor();
		if (nd != null) {
			mDialog.setMessage(nd.getInfo(requireActivity()));
		}
		switch (status) {
			case AppInstaller.STATUS_SUCCESS:
				installed++;
				nextFile();
				return;
			case AppInstaller.STATUS_NEW:
			case AppInstaller.STATUS_OLDEST:
			case AppInstaller.STATUS_EQUAL:
			case AppInstaller.STATUS_NEWEST:
				convert();
				return;
			default:
				if ((status == AppInstaller.STATUS_NEED_JAD || status == AppInstaller.STATUS_UNMATCHED)
						&& installFromSelectedJar()) {
					return;
				}
				failCurrentFile();
		}
	}

	private boolean installFromSelectedJar() {
		if (jarAttempted) {
			return false;
		}
		jarAttempted = true;
		Uri jar = findSelectedJar();
		if (jar == null) {
			return false;
		}
		if (uriQueue.remove(jar)) {
			processed++;
		}
		Disposable disposable = installer.updateInfo(jar)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
		return true;
	}

	@Nullable
	private Uri findSelectedJar() {
		Descriptor nd = installer.getNewDescriptor();
		if (nd == null) {
			return null;
		}
		Set<String> candidates = new HashSet<>();
		String jarUrl = nd.getJarUrl();
		if (jarUrl != null) {
			int i = Math.max(jarUrl.lastIndexOf('/'), jarUrl.lastIndexOf('\\'));
			candidates.add(jarUrl.substring(i + 1).toLowerCase());
		}
		String current = currentSourceName.toLowerCase();
		int dot = current.lastIndexOf('.');
		if (dot > 0) {
			candidates.add(current.substring(0, dot) + ".jar");
		}
		Uri processedUri = null;
		for (Map.Entry<Uri, String> entry : uriNames.entrySet()) {
			String name = entry.getValue().toLowerCase();
			if (!name.endsWith(".jar") || !candidates.contains(name)) {
				continue;
			}
			Uri uri = entry.getKey();
			if (uriQueue.contains(uri)) {
				return uri;
			}
			if (processedUri == null) {
				processedUri = uri;
			}
		}
		return processedUri;
	}

	private void convert() {
		if (installer.getJar() == null) {
			SpannableStringBuilder info = installer.getNewDescriptor().getInfo(requireActivity());
			info.append(getString(R.string.warn_install_from_net));
			mDialog.setMessage(info);
		}
		binding.installationStatus.setText(R.string.converting_wait);
		showProgress();
		Disposable disposable = Single.create(installer::install)
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onProgress, this::onError);
		compositeDisposable.add(disposable);
	}

	private void failCurrentFile() {
		addFailedName();
		installer.deleteTemp();
		installer.clearCache();
		nextFile();
	}

	private void addFailedName() {
		String name = currentSourceName;
		Descriptor nd = installer.getNewDescriptor();
		if (nd != null && nd.getName() != null && !nd.getName().isEmpty()) {
			name = nd.getName();
		}
		failedNames.add(name);
	}

	private void onError(Throwable e) {
		e.printStackTrace();
		if (!isAdded() || cancelled) {
			return;
		}
		addFailedName();
		installer.deleteTemp();
		installer.clearCache();
		nextFile();
	}

	private void onFinished() {
		finished = true;
		SpannableStringBuilder message = new SpannableStringBuilder(
				getString(R.string.install_multiple_done, installed));
		if (!failedNames.isEmpty()) {
			message.append('\n');
			message.append(getString(R.string.install_multiple_failed, failedNames.size()));
			int count = Math.min(failedNames.size(), MAX_FAILED_LIST_SIZE);
			for (int i = 0; i < count; i++) {
				message.append('\n').append(failedNames.get(i));
			}
			if (failedNames.size() > count) {
				message.append('\n').append(getString(R.string.install_multiple_more,
						failedNames.size() - count));
			}
		}
		mDialog.setMessage(message);
		hideProgress();
		btnClose.setText(R.string.close);
	}

	private void cancel() {
		cancelled = true;
		compositeDisposable.clear();
		dismiss();
	}

	private void hideProgress() {
		binding.installationProgress.setVisibility(View.GONE);
		binding.installationStatus.setVisibility(View.GONE);
	}

	private void showProgress() {
		binding.installationProgress.setVisibility(View.VISIBLE);
		binding.installationStatus.setVisibility(View.VISIBLE);
	}
}
