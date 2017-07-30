package ua.naiksoftware.j2meloader;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

import javax.microedition.shell.ConfigActivity;

import ua.naiksoftware.util.FileUtils;

/**
 * @author Naik
 */
public class AppsListFragment extends ListFragment {

	private ArrayList<AppItem> apps;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		apps = (ArrayList<AppItem>) getArguments().getSerializable("apps");
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getText(R.string.no_data_for_display));
		registerForContextMenu(getListView());
	}

	private void showDialog(final int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(android.R.string.dialog_alert_title)
				.setMessage(R.string.message_delete)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						File appDir = new File(apps.get(id).getPath());
						FileUtils.deleteDirectory(appDir);
						File appSaveDir = new File(getActivity().getFilesDir(), apps.get(id).getTitle());
						FileUtils.deleteDirectory(appSaveDir);
						File appSettings = new File(getActivity().getFilesDir().getParent() + File.separator + "shared_prefs", apps.get(id).getTitle() + ".xml");
						appSettings.delete();
						((MainActivity) getActivity()).updateApps();
					}
				})
				.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		AppItem item = apps.get(position);
		Intent i = new Intent(Intent.ACTION_DEFAULT, Uri.parse(item.getPath()), getActivity(), ConfigActivity.class);
		i.putExtra("name", item.getTitle());
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
		AppItem appItem = apps.get(index);
		switch (item.getItemId()) {
			case R.id.action_context_shortcut:
				Bitmap bitmap = BitmapFactory.decodeFile(appItem.getImagePath());
				Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
				Intent launchIntent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appItem.getPath()), getActivity(), ConfigActivity.class);
				launchIntent.putExtra("name", appItem.getTitle());
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appItem.getTitle());
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
				getActivity().sendBroadcast(intent);
				break;
			case R.id.action_context_settings:
				Intent i = new Intent(Intent.ACTION_DEFAULT, Uri.parse(appItem.getPath()), getActivity(), ConfigActivity.class);
				i.putExtra("name", appItem.getTitle());
				i.putExtra("showSettings", true);
				startActivity(i);
				break;
			case R.id.action_context_delete:
				showDialog(index);
				break;
		}
		return super.onContextItemSelected(item);
	}

}
