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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;
import ru.playsoftware.j2meloader.util.FileUtils;

public class TemplatesActivity extends BaseActivity {

	private TemplatesAdapter adapter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_templates);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.templates);

		ArrayList<String> templates = getTemplatesList();
		ListView listView = findViewById(R.id.list_view);
		TextView emptyView = findViewById(R.id.empty_view);
		listView.setEmptyView(emptyView);
		registerForContextMenu(listView);
		adapter = new TemplatesAdapter(this, templates);
		listView.setAdapter(adapter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_tempates, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int index = info.position;
		switch (item.getItemId()) {
			case R.id.action_context_rename:
				showRenameDialog(index);
				break;
			case R.id.action_context_delete:
				deleteTemplate(index);
				break;
		}
		return super.onContextItemSelected(item);
	}

	private void deleteTemplate(final int id) {
		File templateDir = new File(Config.TEMPLATES_DIR, (String) adapter.getItem(id));
		FileUtils.deleteDirectory(templateDir);
		adapter.removeItem(id);
	}

	private void renameTemplate(final int id, String newName) {
		File srcTemplateDir = new File(Config.TEMPLATES_DIR, (String) adapter.getItem(id));
		File dstTemplateDir = new File(Config.TEMPLATES_DIR, newName);
		srcTemplateDir.renameTo(dstTemplateDir);
		adapter.renameItem(id, dstTemplateDir.getName());
	}

	private ArrayList<String> getTemplatesList() {
		File templatesDir = new File(Config.TEMPLATES_DIR);
		File[] templatesList = templatesDir.listFiles();
		if (templatesList == null) {
			return new ArrayList<String>();
		}
		int size = templatesList.length;
		String[] templates = new String[size];
		for (int i = 0; i < size; i++) {
			templates[i] = templatesList[i].getName();
		}
		return new ArrayList(Arrays.asList(templates));
	}

	private void showRenameDialog(final int id) {
		String name = (String) adapter.getItem(id);
		EditText editText = new EditText(this);
		editText.setText(name);
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.action_context_rename)
				.setView(editText)
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					String newName = editText.getText().toString().trim();
					if (newName.equals("")) {
						Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
					} else {
						renameTemplate(id, newName);
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}
}
