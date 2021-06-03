/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader.applist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ConfigActivity;
import ru.playsoftware.j2meloader.config.ProfilesActivity;
import ru.playsoftware.j2meloader.donations.DonationsActivity;
import ru.playsoftware.j2meloader.filepicker.FilteredFilePickerActivity;
import ru.playsoftware.j2meloader.filepicker.FilteredFilePickerFragment;
import ru.playsoftware.j2meloader.info.AboutDialogFragment;
import ru.playsoftware.j2meloader.info.HelpDialogFragment;
import ru.playsoftware.j2meloader.settings.SettingsActivity;
import ru.playsoftware.j2meloader.util.AppUtils;
import ru.playsoftware.j2meloader.util.JarConverter;
import ru.playsoftware.j2meloader.util.LogUtils;

import static ru.playsoftware.j2meloader.util.Constants.*;

public class AppsListFragment extends ListFragment {

	private AppRepository appRepository;
	private CompositeDisposable compositeDisposable;
	private AppsListAdapter adapter;
	private JarConverter converter;
	private String appSort;
	private Uri appPath;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		compositeDisposable = new CompositeDisposable();
		converter = new JarConverter(getActivity().getApplicationInfo().dataDir);
		adapter = new AppsListAdapter(getActivity());
		Bundle args = getArguments();
		if (args == null) {
			return;
		}
		appSort = args.getString(KEY_APP_SORT);
		appPath = args.getParcelable(KEY_APP_PATH);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_appslist, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
		setListAdapter(adapter);
		initDb();
		FloatingActionButton fab = getActivity().findViewById(R.id.fab);
		fab.setOnClickListener(v -> {
			Intent i = new Intent(getActivity(), FilteredFilePickerActivity.class);
			i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
			i.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, true);
			i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
			i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
			i.putExtra(FilePickerActivity.EXTRA_START_PATH, FilteredFilePickerFragment.getLastPath());
			startActivityForResult(i, REQUEST_FILE);
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (appPath != null) {
			convertJar(appPath);
			appPath = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		compositeDisposable.clear();
	}

	@SuppressLint("CheckResult")
	private void initDb() {
		appRepository = new AppRepository(getActivity().getApplication(), appSort.equals("date"));
		ConnectableFlowable<List<AppItem>> listConnectableFlowable = appRepository.getAll()
				.subscribeOn(Schedulers.io())
				.doOnError(this::alertDbError)
				.publish();
		listConnectableFlowable
				.firstElement()
				.subscribe(list -> AppUtils.updateDb(appRepository, list), this::alertDbError);
		listConnectableFlowable
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(list -> adapter.setItems(list), this::alertDbError);
		compositeDisposable.add(listConnectableFlowable.connect());
	}

	private void alertDbError(Throwable throwable) {
		if (throwable instanceof SQLiteDiskIOException) {
			requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
					R.string.error_disk_io, Toast.LENGTH_SHORT).show());
		} else {
			requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
					getString(R.string.error) + ": " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_FILE && resultCode == Activity.RESULT_OK) {
			List<Uri> files = Utils.getSelectedFilesFromResult(data);
			for (Uri uri : files) {
				File file = Utils.getFileForUri(uri);
				convertJar(Uri.fromFile(file));
			}
		}
	}

	@SuppressLint("CheckResult")
	private void convertJar(Uri path) {
		ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.setMessage(getText(R.string.converting_message));
		dialog.setTitle(R.string.converting_wait);
		converter.convert(path.buildUpon().build())
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribeWith(new SingleObserver<String>() {
					@Override
					public void onSubscribe(Disposable d) {
						dialog.show();
					}

					@Override
					public void onSuccess(String s) {
						AppItem app = AppUtils.getApp(s);
						appRepository.insert(app);
						if (!isAdded()) return;
						dialog.dismiss();
						showStartDialog(app);
					}

					@Override
					public void onError(Throwable e) {
						e.printStackTrace();
						if (!isAdded()) return;
						Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
						dialog.dismiss();
					}
				});
	}

	private void showStartDialog(AppItem app) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		StringBuilder text = new StringBuilder()
				.append(getString(R.string.author)).append(' ')
				.append(app.getAuthor()).append('\n')
				.append(getString(R.string.version)).append(' ')
				.append(app.getVersion()).append('\n');
		dialog.setMessage(text);
		dialog.setTitle(app.getTitle());
		Drawable drawable = Drawable.createFromPath(app.getImagePathExt());
		if (drawable != null) dialog.setIcon(drawable);
		dialog.setPositiveButton(R.string.START_CMD, (d, w) -> {
			Config.startApp(getActivity(), app.getTitle(), app.getPathExt(), false);
		});
		dialog.setNegativeButton(R.string.close, null);
		dialog.show();
	}

	private void showRenameDialog(final int id) {
		AppItem item = adapter.getItem(id);
		EditText editText = new EditText(getActivity());
		editText.setText(item.getTitle());
		float density = getResources().getDisplayMetrics().density;
		LinearLayout linearLayout = new LinearLayout(getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		int margin = (int) (density * 20);
		params.setMargins(margin, 0, margin, 0);
		linearLayout.addView(editText, params);
		int paddingVertical = (int) (density * 16);
		int paddingHorizontal = (int) (density * 8);
		editText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.action_context_rename)
				.setView(linearLayout)
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					String title = editText.getText().toString().trim();
					if (title.equals("")) {
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					} else {
						item.setTitle(title);
						appRepository.insert(item);
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	private void showDeleteDialog(final int id) {
		AppItem item = adapter.getItem(id);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_delete)
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					AppUtils.deleteApp(item);
					appRepository.delete(item);
				})
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	@Override
	public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
		AppItem item = adapter.getItem(position);
		Config.startApp(getActivity(), item.getTitle(), item.getPathExt(), false);
	}

	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
									ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.context_main, menu);
		if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
			menu.findItem(R.id.action_context_shortcut).setVisible(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int index = info.position;
		AppItem appItem = adapter.getItem(index);
		int itemId = item.getItemId();
		if (itemId == R.id.action_context_shortcut) {
			requestAddShortcut(appItem);
		} else if (itemId == R.id.action_context_rename) {
			showRenameDialog(index);
		} else if (itemId == R.id.action_context_settings) {
			Config.startApp(getActivity(), appItem.getTitle(), appItem.getPathExt(), true);
		} else if (itemId == R.id.action_context_delete) {
			showDeleteDialog(index);
		} else {
			return super.onContextItemSelected(item);
		}
		return true;
	}

	private void requestAddShortcut(AppItem appItem) {
		FragmentActivity activity = requireActivity();
		Bitmap bitmap = BitmapFactory.decodeFile(appItem.getImagePathExt());
		IconCompat icon;
		if (bitmap == null) {
			icon = IconCompat.createWithResource(activity, R.mipmap.ic_launcher);
		} else {
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
			int iconSize = am.getLauncherLargeIconSize();
			Rect src;
			if (width > height) {
				int left = (width - height) / 2;
				src = new Rect(left, 0, left + height, height);
			} else if (width < height) {
				int top = (height - width) / 2;
				src = new Rect(0, top, width, top + width);
			} else {
				src = null;
			}
			Bitmap scaled = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(scaled);
			canvas.drawBitmap(bitmap, src, new RectF(0, 0, iconSize, iconSize), null);
			icon = IconCompat.createWithBitmap(scaled);
		}
		String title = appItem.getTitle();
		Intent launchIntent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appItem.getPathExt()),
				activity, ConfigActivity.class);
		launchIntent.putExtra(KEY_MIDLET_NAME, title);
		ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(activity, title)
				.setIntent(launchIntent)
				.setShortLabel(title)
				.setIcon(icon)
				.build();
		ShortcutManagerCompat.requestPinShortcut(activity, shortcut, null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main, menu);
		final MenuItem searchItem = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) searchItem.getActionView();
		Disposable searchViewDisposable = Observable.create((ObservableOnSubscribe<String>) emitter -> {
			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String query) {
					emitter.onNext(query);
					return true;
				}

				@Override
				public boolean onQueryTextChange(String newText) {
					emitter.onNext(newText);
					return true;
				}
			});
		}).debounce(300, TimeUnit.MILLISECONDS)
				.map(String::toLowerCase)
				.distinctUntilChanged()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(charSequence -> adapter.getFilter().filter(charSequence));
		compositeDisposable.add(searchViewDisposable);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_about) {
			AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
			aboutDialogFragment.show(getChildFragmentManager(), "about");
		} else if (itemId == R.id.action_settings) {
			Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
			requireActivity().startActivityForResult(settingsIntent, REQUEST_SETTINGS);
		} else if (itemId == R.id.action_profiles) {
			Intent intentProfiles = new Intent(getActivity(), ProfilesActivity.class);
			startActivity(intentProfiles);
		} else if (itemId == R.id.action_help) {
			HelpDialogFragment helpDialogFragment = new HelpDialogFragment();
			helpDialogFragment.show(getChildFragmentManager(), "help");
		} else if (itemId == R.id.action_donate) {
			Intent donationsIntent = new Intent(getActivity(), DonationsActivity.class);
			startActivity(donationsIntent);
		} else if (itemId == R.id.action_save_log) {
			try {
				LogUtils.writeLog();
				Toast.makeText(getActivity(), R.string.log_saved, Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
			}
		} else if (itemId == R.id.action_exit_app) {
			requireActivity().finish();
		}
		return super.onOptionsItemSelected(item);
	}
}
