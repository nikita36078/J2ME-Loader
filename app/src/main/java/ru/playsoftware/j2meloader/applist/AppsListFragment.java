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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.File;

import javax.microedition.shell.ConfigActivity;

import ru.playsoftware.j2meloader.MainActivity;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.util.FileUtils;

public class AppsListFragment extends ListFragment {

	private AppsListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		super.setListAdapter(adapter);
		this.adapter = (AppsListAdapter) adapter;
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getText(R.string.no_data_for_display));
		registerForContextMenu(getListView());
	}

	private void showDeleteDialog(final int id) {
		AppItem item = (AppItem) adapter.getItem(id);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_delete)
				.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
					File appDir = new File(item.getPathExt());
					FileUtils.deleteDirectory(appDir);
					File appSaveDir = new File(ConfigActivity.DATA_DIR, item.getTitle());
					FileUtils.deleteDirectory(appSaveDir);
					File appSettings = new File(getActivity().getFilesDir().getParent() +
							File.separator + "shared_prefs", item.getTitle() + ".xml");
					appSettings.delete();
					((MainActivity) getActivity()).deleteApp(item);
				})
				.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		AppItem item = (AppItem) adapter.getItem(position);
		Intent i = new Intent(Intent.ACTION_DEFAULT, Uri.parse(item.getPathExt()), getActivity(), ConfigActivity.class);
		i.putExtra(ConfigActivity.MIDLET_NAME_KEY, item.getTitle());
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
				Intent launchIntent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appItem.getPathExt()), getActivity(), ConfigActivity.class);
				launchIntent.putExtra(ConfigActivity.MIDLET_NAME_KEY, appItem.getTitle());
				ShortcutInfoCompat.Builder shortcutInfoCompatBuilder = new ShortcutInfoCompat.Builder(getActivity(), appItem.getTitle())
						.setIntent(launchIntent)
						.setShortLabel(appItem.getTitle());
				if (bitmap != null) {
					shortcutInfoCompatBuilder.setIcon(IconCompat.createWithBitmap(bitmap));
				} else {
					shortcutInfoCompatBuilder.setIcon(IconCompat.createWithResource(getActivity(), R.mipmap.ic_launcher));
				}
				ShortcutManagerCompat.requestPinShortcut(getActivity(), shortcutInfoCompatBuilder.build(), null);
				break;
			case R.id.action_context_settings:
				Intent i = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appItem.getPathExt()), getActivity(), ConfigActivity.class);
				i.putExtra(ConfigActivity.MIDLET_NAME_KEY, appItem.getTitle());
				i.putExtra(ConfigActivity.SHOW_SETTINGS_KEY, true);
				startActivity(i);
				break;
			case R.id.action_context_delete:
				showDeleteDialog(index);
				break;
		}
		return super.onContextItemSelected(item);
	}

}
