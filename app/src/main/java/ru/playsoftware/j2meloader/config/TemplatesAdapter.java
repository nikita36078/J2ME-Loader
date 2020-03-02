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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import ru.playsoftware.j2meloader.R;

public class TemplatesAdapter extends BaseAdapter {
	private ArrayList<Template> list;
	private final LayoutInflater layoutInflater;
	private int defaultIndex = -1;

	TemplatesAdapter(Context context, ArrayList<Template> list) {
		if (list != null) {
			this.list = list;
			Collections.sort(list);
		}
		this.layoutInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Template getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup) {
		TemplatesAdapter.ViewHolder holder;
		if (view == null) {
			view = layoutInflater.inflate(R.layout.list_row_template, viewGroup, false);
			holder = new TemplatesAdapter.ViewHolder();
			holder.name = (TextView) view;
			view.setTag(holder);
		} else {
			holder = (TemplatesAdapter.ViewHolder) view.getTag();
		}

		String name = list.get(position).getName();
		if (position == defaultIndex) {
			name = view.getResources().getString(R.string.default_template, name);
		}
		holder.name.setText(name);

		return view;
	}

	void removeItem(int position) {
		list.remove(position);
		notifyDataSetChanged();
	}

	public void setDefault(int index) {
		defaultIndex = index;
		notifyDataSetChanged();
	}

	void addItem(Template template) {
		list.add(template);
		Collections.sort(list);
		notifyDataSetChanged();
	}

	private static class ViewHolder {
		TextView name;
	}
}
