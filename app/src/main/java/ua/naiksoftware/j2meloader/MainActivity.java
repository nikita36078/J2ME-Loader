package ua.naiksoftware.j2meloader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.microedition.shell.ConfigActivity;

import ua.naiksoftware.util.FileUtils;

public class MainActivity extends AppCompatActivity implements
		NavigationDrawerFragment.SelectedCallback {

	private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 0;
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

	/**
	 * путь к папке со сконвертированными приложениями
	 */
	private String pathConverted;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
		} else {
			setupActivity();
		}
		Uri uri = getIntent().getData();
		if (savedInstanceState == null && uri != null) {
			JarConverter converter = new JarConverter(this);
			try {
				converter.execute(FileUtils.getPath(this, uri), pathConverted);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void setupActivity() {
		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		pathConverted = getApplicationInfo().dataDir + "/converted/";
		appsListFragment = new AppsListFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable("apps", apps);
		appsListFragment.setArguments(bundle);
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.container, appsListFragment).commit();
		updateApps();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_WRITE_STORAGE: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					setupActivity();
				} else {
					Toast.makeText(this, R.string.permission_request_failed, Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
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
				AboutDialogFragment dialogFragment = new AboutDialogFragment();
				dialogFragment.show(getSupportFragmentManager(), "about");
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
		converter.execute(path, pathConverted);
	}

	public void updateApps() {
		apps.clear();
		AppItem item;
		String author = getString(R.string.author);
		String version = getString(R.string.version);
		String[] appFolders = new File(pathConverted).list();
		if (!(appFolders == null)) {
			for (String appFolder : appFolders) {
				File temp = new File(pathConverted + appFolder);
				if (temp.list().length > 0) {
					LinkedHashMap<String, String> params = FileUtils
							.loadManifest(new File(temp.getAbsolutePath(), ConfigActivity.MIDLET_CONF_FILE));
					item = new AppItem(getIcon(params.get("MIDlet-1")),
							params.get("MIDlet-Name"),
							author + params.get("MIDlet-Vendor"),
							version + params.get("MIDlet-Version"));
					item.setPath(pathConverted + appFolder);
					apps.add(item);
				} else {
					temp.delete();
				}
			}
		}
		AppsListAdapter adapter = new AppsListAdapter(this, apps);
		appsListFragment.setListAdapter(adapter);
	}

	private String getIcon(String input) {
		String[] params = input.split(",");
		return params[1];
	}

}
