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

package ua.naiksoftware.j2meloader;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

import javax.microedition.shell.ConfigActivity;

import ua.naiksoftware.util.FileUtils;

public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.SelectedCallback {

	public static final String APP_LIST_KEY = "apps";
	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	private AppsListFragment appsListFragment;
	private ArrayList<AppItem> apps = new ArrayList<AppItem>();
	private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 0;
	private static final Comparator<SortItem> comparator = new AlphabeticComparator<SortItem>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Uri uri = getIntent().getData();
		if (!isTaskRoot() && uri == null) {
			finish();
			return;
		}
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			Toast.makeText(this, R.string.external_storage_not_mounted, Toast.LENGTH_SHORT).show();
			finish();
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
		} else {
			setupActivity();
			if (savedInstanceState == null && uri != null) {
				JarConverter converter = new JarConverter(this);
				try {
					converter.execute(FileUtils.getJarPath(this, uri), ConfigActivity.APP_DIR);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void setupActivity() {
		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
		moveToNewLocation();
		appsListFragment = new AppsListFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(APP_LIST_KEY, apps);
		appsListFragment.setArguments(bundle);
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.container, appsListFragment).commitAllowingStateLoss();
		updateApps();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_WRITE_STORAGE:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					setupActivity();
				} else {
					Toast.makeText(this, R.string.permission_request_failed, Toast.LENGTH_SHORT).show();
					finish();
				}
				break;
		}
	}

	private void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	private void moveToNewLocation() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		boolean dataMoved = sp.getBoolean("pref_data_moved", false);
		if (!dataMoved) {
			File oldConvertedDir = new File(getApplicationInfo().dataDir, ConfigActivity.MIDLET_DIR);
			File oldDataDir = getFilesDir();
			if (oldConvertedDir.exists() && oldConvertedDir.listFiles().length > 0) {
				FileUtils.moveFiles(oldConvertedDir.getPath(), ConfigActivity.APP_DIR, null);
				FileUtils.deleteDirectory(oldConvertedDir);

				if (oldDataDir.exists() && oldDataDir.listFiles().length > 0) {
					FileUtils.moveFiles(oldDataDir.getPath(), ConfigActivity.DATA_DIR, new FilenameFilter() {
						@Override
						public boolean accept(File file, String s) {
							return !s.endsWith(".stacktrace");
						}
					});
					FileUtils.deleteDirectory(oldDataDir);
				}
			}
			sp.edit().putBoolean("pref_data_moved", true).apply();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			restoreActionBar();
		}
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_about:
				AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
				aboutDialogFragment.show(getSupportFragmentManager(), "about");
				break;
			case R.id.action_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			case R.id.action_help:
				HelpDialogFragment helpDialogFragment = new HelpDialogFragment();
				helpDialogFragment.show(getSupportFragmentManager(), "help");
				break;
			case R.id.action_exit_app:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSelected(String path) {
		JarConverter converter = new JarConverter(this);
		converter.execute(path, ConfigActivity.APP_DIR);
	}

	public void updateApps() {
		apps.clear();
		AppItem item;
		String author = getString(R.string.author);
		String version = getString(R.string.version);
		String[] appFolders = new File(ConfigActivity.APP_DIR).list();
		if (!(appFolders == null)) {
			for (String appFolder : appFolders) {
				File temp = new File(ConfigActivity.APP_DIR, appFolder);
				if (temp.list().length > 0) {
					LinkedHashMap<String, String> params = FileUtils
							.loadManifest(new File(temp.getAbsolutePath(), ConfigActivity.MIDLET_CONF_FILE));
					item = new AppItem(getIcon(params.get("MIDlet-1")),
							params.get("MIDlet-Name"),
							author + params.get("MIDlet-Vendor"),
							version + params.get("MIDlet-Version"));
					item.setPath(ConfigActivity.APP_DIR + appFolder);
					apps.add(item);
				} else {
					temp.delete();
				}
			}
		}
		Collections.sort(apps, comparator);
		AppsListAdapter adapter = new AppsListAdapter(this, apps);
		appsListFragment.setListAdapter(adapter);
	}

	private String getIcon(String input) {
		String[] params = input.split(",");
		return params[1];
	}

}
