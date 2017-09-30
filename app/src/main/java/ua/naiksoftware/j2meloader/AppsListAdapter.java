/*
 * J2ME Loader
 * Copyright (C) 2015-2016 Nickolay Savchenko
 * Copyright (C) 2017 Nikita Shakarun
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
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppsListAdapter extends BaseAdapter {

	private List<AppItem> list;
	private final LayoutInflater layoutInflater;
	private Context context;

	public AppsListAdapter(Context context, List<AppItem> list) {
		if (list != null) {
			this.list = list;
		}
		this.layoutInflater = LayoutInflater.from(context);
		this.context = context;
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
			view = layoutInflater.inflate(R.layout.list_row_jar, null);
			holder = new ViewHolder();
			holder.icon = (ImageView) view.findViewById(R.id.list_image);
			holder.name = (TextView) view.findViewById(R.id.list_title);
			holder.author = (TextView) view.findViewById(R.id.list_author);
			holder.version = (TextView) view.findViewById(R.id.list_version);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}
		AppItem item = list.get(position);

		holder.icon.setImageDrawable(new BitmapDrawable(context.getResources(), item.getImagePath()));
		holder.name.setText(item.getTitle());
		holder.author.setText(item.getAuthor());
		holder.version.setText(item.getVersion());

		return view;
	}

	private static class ViewHolder {
		ImageView icon;
		TextView name;
		TextView author;
		TextView version;
	}
}
