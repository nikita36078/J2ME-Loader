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
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;

public class TemplatesActivity extends BaseActivity {

	private TemplatesAdapter adapter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_templates);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.templates);

		ArrayList<Template> templates = TemplatesManager.getTemplatesList();
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
		Template template = (Template) adapter.getItem(id);
		template.delete();
		adapter.removeItem(id);
	}

	private void showRenameDialog(final int id) {
		Template template = (Template) adapter.getItem(id);
		EditText editText = new EditText(this);
		editText.setText(template.getName());
		float density = getResources().getDisplayMetrics().density;
		LinearLayout linearLayout = new LinearLayout(this);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		int margin = (int) (density * 20);
		params.setMargins(margin, 0, margin, 0);
		linearLayout.addView(editText, params);
		int paddingVertical = (int) (density * 16);
		int paddingHorizontal = (int) (density * 8);
		editText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.action_context_rename)
				.setView(linearLayout)
				.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
					String newName = editText.getText().toString().trim();
					if (newName.equals("")) {
						Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
					} else {
						template.renameTo(newName);
						adapter.renameItem(id, template);
					}
				})
				.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}
}
