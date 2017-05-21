package ua.naiksoftware.j2meloader;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.param.DataContainer;
import javax.microedition.param.SharedPreferencesContainer;
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
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                showDialog(i);
                return true;
            }
        });
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
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        return;
                    }
                });
        builder.show();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        AppItem item = apps.get(position);
        Intent i = new Intent(Intent.ACTION_DEFAULT, Uri.parse(item.getPath()), getActivity(), ConfigActivity.class);
        i.putExtra("name", item.getTitle());
        startActivityForResult(i, 0);
    }

}
