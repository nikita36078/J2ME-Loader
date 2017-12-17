/*
 * J2ME Loader
 * Copyright (C) 2015-2016 Nickolay Savchenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ua.naiksoftware.j2meloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class FileListAdapter extends BaseAdapter {

	private ArrayList<FSItem> list = new ArrayList<FSItem>();
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
			view = li.inflate(R.layout.list_row, null);
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
