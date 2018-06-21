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

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.playsoftware.j2meloader.MainActivity;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.appsdb.AppDatabase;
import ru.playsoftware.j2meloader.appsdb.AppItemDao;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.config.ConfigActivity;
import ru.playsoftware.j2meloader.config.TemplatesActivity;
import ru.playsoftware.j2meloader.donations.DonationsActivity;
import ru.playsoftware.j2meloader.filepicker.FilteredFilePickerActivity;
import ru.playsoftware.j2meloader.filepicker.FilteredFilePickerFragment;
import ru.playsoftware.j2meloader.info.AboutDialogFragment;
import ru.playsoftware.j2meloader.info.HelpDialogFragment;
import ru.playsoftware.j2meloader.settings.SettingsActivity;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.playsoftware.j2meloader.util.JarConverter;

public class AppsListFragment extends ListFragment {

	private AppItemDao appItemDao;
	private AppsListAdapter adapter;
	private String appSort;
	private static final int FILE_CODE = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_appslist, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		appSort = getArguments().getString(MainActivity.APP_SORT_KEY);
		ArrayList<AppItem> apps = new ArrayList<>();
		adapter = new AppsListAdapter(getActivity(), apps);
		setListAdapter(adapter);
		initDb();
		registerForContextMenu(getListView());
		setHasOptionsMenu(true);
		FloatingActionButton fab = getActivity().findViewById(R.id.fab);
		fab.setOnClickListener(v -> {
			Intent i = new Intent(getActivity(), FilteredFilePickerActivity.class);
			i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
			i.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, true);
			i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
			i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
			i.putExtra(FilePickerActivity.EXTRA_START_PATH, FilteredFilePickerFragment.getLastPath());
			startActivityForResult(i, FILE_CODE);
		});
	}

	public void addApp(AppItem item) {
		appItemDao.insert(item);
		updateAppsList();
	}

	public void deleteApp(AppItem item) {
		appItemDao.delete(item);
		updateAppsList();
	}

	public void deleteAllApps() {
		appItemDao.deleteAll();
	}

	private void updateAppsList() {
		List<AppItem> apps;
		if (appSort.equals("name")) {
			apps = appItemDao.getAllByName();
		} else {
			apps = appItemDao.getAllByDate();
		}
		adapter.setItems(apps);
	}

	private void initDb() {
		AppDatabase db = Room.databaseBuilder(getActivity(),
				AppDatabase.class, "apps-database.db").allowMainThreadQueries().build();
		appItemDao = db.appItemDao();
		if (!FileUtils.checkDb(this, appItemDao.getAllByName())) {
			appItemDao.insertAll(FileUtils.getAppsList(getActivity()));
		}
		updateAppsList();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
			List<Uri> files = Utils.getSelectedFilesFromResult(data);
			for (Uri uri : files) {
				File file = Utils.getFileForUri(uri);
				convertJar(file.getAbsolutePath());
			}
		}
	}

	public void convertJar(String path) {
		JarConverter converter = new JarConverter(this);
		converter.execute(path);
	}

	private void showRenameDialog(final int id) {
		AppItem item = (AppItem) adapter.getItem(id);
		EditText editText = new EditText(getActivity());
		editText.setText(item.getTitle());
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.action_context_rename)
				.setView(editText)
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					String title = editText.getText().toString().trim();
					if (title.equals("")) {
						Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
					} else {
						item.setTitle(title);
						addApp(item);
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	private void showDeleteDialog(final int id) {
		AppItem item = (AppItem) adapter.getItem(id);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_delete)
				.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
					File appDir = new File(item.getPathExt());
					FileUtils.deleteDirectory(appDir);
					File appSaveDir = new File(Config.DATA_DIR, item.getTitle());
					FileUtils.deleteDirectory(appSaveDir);
					File appConfigsDir = new File(Config.CONFIGS_DIR, item.getTitle());
					FileUtils.deleteDirectory(appConfigsDir);
					deleteApp(item);
				})
				.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		AppItem item = (AppItem) adapter.getItem(position);
		Intent i = new Intent(Intent.ACTION_DEFAULT, Uri.parse(item.getPathExt()), getActivity(), ConfigActivity.class);
		startActivity(i);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.context_main, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int index = info.position;
		AppItem appItem = (AppItem) adapter.getItem(index);
		switch (item.getItemId()) {
			case R.id.action_context_shortcut:
				Bitmap bitmap = BitmapFactory.decodeFile(appItem.getImagePathExt());
				Intent launchIntent = new Intent(Intent.ACTION_DEFAULT,
						Uri.parse(appItem.getPathExt()), getActivity(), ConfigActivity.class);
				ShortcutInfoCompat.Builder shortcutInfoCompatBuilder =
						new ShortcutInfoCompat.Builder(getActivity(), appItem.getTitle())
								.setIntent(launchIntent)
								.setShortLabel(appItem.getTitle());
				if (bitmap != null) {
					shortcutInfoCompatBuilder.setIcon(IconCompat.createWithBitmap(bitmap));
				} else {
					shortcutInfoCompatBuilder.setIcon(IconCompat.createWithResource(getActivity(), R.mipmap.ic_launcher));
				}
				ShortcutManagerCompat.requestPinShortcut(getActivity(), shortcutInfoCompatBuilder.build(), null);
				break;
			case R.id.action_context_rename:
				showRenameDialog(index);
				break;
			case R.id.action_context_settings:
				Intent i = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appItem.getPathExt()), getActivity(), ConfigActivity.class);
				i.putExtra(ConfigActivity.SHOW_SETTINGS_KEY, true);
				startActivity(i);
				break;
			case R.id.action_context_delete:
				showDeleteDialog(index);
				break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_about:
				AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
				aboutDialogFragment.show(getFragmentManager(), "about");
				break;
			case R.id.action_settings:
				Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
				startActivity(settingsIntent);
				break;
			case R.id.action_templates:
				Intent templatesIntent = new Intent(getActivity(), TemplatesActivity.class);
				startActivity(templatesIntent);
				break;
			case R.id.action_help:
				HelpDialogFragment helpDialogFragment = new HelpDialogFragment();
				helpDialogFragment.show(getFragmentManager(), "help");
				break;
			case R.id.action_donate:
				Intent donationsIntent = new Intent(getActivity(), DonationsActivity.class);
				startActivity(donationsIntent);
				break;
			case R.id.action_exit_app:
				getActivity().finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

}
