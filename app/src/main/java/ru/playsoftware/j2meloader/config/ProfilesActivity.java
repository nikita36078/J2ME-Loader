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

package ru.playsoftware.j2meloader.config;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceManager;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;

import static ru.playsoftware.j2meloader.util.Constants.*;

public class ProfilesActivity extends BaseActivity implements EditNameDialog.Callback, AdapterView.OnItemClickListener {
	private ProfilesAdapter adapter;
	private SharedPreferences preferences;
	private final ActivityResultLauncher<String> editProfileLauncher = registerForActivityResult(
			new ActivityResultContract<String, String>() {
				@NonNull
				@Override
				public Intent createIntent(@NonNull Context context, String input) {
					return new Intent(ACTION_EDIT_PROFILE, Uri.parse(input),
							getApplicationContext(), ConfigActivity.class);
				}

				@Override
				public String parseResult(int resultCode, @Nullable Intent intent) {
					if (resultCode == Activity.RESULT_OK && intent != null) {
						return intent.getDataString();
					}
					return null;
				}
			},
			name -> {
				if (name != null) {
					adapter.addItem(new Profile(name));
				}
			});

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profiles);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		setTitle(R.string.profiles);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		ArrayList<Profile> profiles = ProfilesManager.getProfiles();
		ListView listView = findViewById(R.id.list_view);
		TextView emptyView = findViewById(R.id.empty_view);
		listView.setEmptyView(emptyView);
		registerForContextMenu(listView);
		adapter = new ProfilesAdapter(getLayoutInflater(), profiles);
		listView.setAdapter(adapter);
		final String def = preferences.getString(PREF_DEFAULT_PROFILE, null);
		if (def != null) {
			for (int i = profiles.size() - 1; i >= 0; i--) {
				Profile profile = profiles.get(i);
				if (profile.getName().equals(def)) {
					adapter.setDefault(profile);
					break;
				}
			}
		}
		listView.setOnItemClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_profiles, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			return true;
		} else if (itemId == R.id.add) {
			EditNameDialog.newInstance(getString(R.string.enter_name), -1, null)
					.show(getSupportFragmentManager(), "alert_create_profile");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.profile, menu);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final Profile profile = adapter.getItem(info.position);
		if (!profile.hasConfig() && !profile.hasOldConfig()) {
			menu.findItem(R.id.action_context_default).setVisible(false);
			menu.findItem(R.id.action_context_edit).setVisible(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int index = info.position;
		final Profile profile = adapter.getItem(index);
		int itemId = item.getItemId();
		if (itemId == R.id.action_context_default) {
			preferences.edit().putString(PREF_DEFAULT_PROFILE, profile.getName()).apply();
			adapter.setDefault(profile);
			return true;
		} else if (itemId == R.id.action_context_edit) {
			final Intent intent = new Intent(ACTION_EDIT_PROFILE,
					Uri.parse(profile.getName()),
					getApplicationContext(), ConfigActivity.class);
			startActivity(intent);
			return true;
		} else if (itemId == R.id.action_context_rename) {
			EditNameDialog.newInstance(getString(R.string.enter_new_name), index, profile.getName())
					.show(getSupportFragmentManager(), "alert_rename_profile");
		} else if (itemId == R.id.action_context_delete) {
			profile.delete();
			adapter.removeItem(index);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onNameChanged(int id, String newName) {
		if (id == -1) {
			editProfileLauncher.launch(newName);
			return;
		}
		Profile profile = adapter.getItem(id);
		profile.renameTo(newName);
		adapter.notifyDataSetChanged();
		if (adapter.getDefault() == profile) {
			preferences.edit().putString(PREF_DEFAULT_PROFILE, newName).apply();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		parent.showContextMenuForChild(view);
	}
}
