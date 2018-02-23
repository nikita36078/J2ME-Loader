/*
 * Copyright 2015-2016 Nickolay Savchenko
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

package ru.playsoftware.j2meloader.filelist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ru.playsoftware.j2meloader.R;

import java.util.ArrayList;

public class FileListAdapter extends BaseAdapter {

	private ArrayList<FSItem> list = new ArrayList<>();
	private final LayoutInflater li;

	public FileListAdapter(Context context, ArrayList<FSItem> arr) {
		if (arr != null) {
			list = arr;
		}
		li = LayoutInflater.from(context);
	}

	public int getCount() {
		return list.size();
	}

	public Object getItem(int position) {
		return list.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View view, ViewGroup viewGroup) {
		ViewHolder holder;
		if (view == null) {
			view = li.inflate(R.layout.list_row, viewGroup);
			holder = new ViewHolder();
			holder.icon = view.findViewById(R.id.list_image);
			holder.label = view.findViewById(R.id.list_header);
			holder.sublabel = view.findViewById(R.id.list_subheader);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		FSItem item = list.get(position);

		holder.icon.setImageResource(item.getImageId());
		holder.label.setText(item.getName());
		holder.sublabel.setText(item.getDescription());

		return view;
	}

	private static class ViewHolder {
		ImageView icon;
		TextView label;
		TextView sublabel;
	}
}
