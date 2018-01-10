/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017 Nikita Shakarun
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
			holder.icon = view.findViewById(R.id.list_image);
			holder.name = view.findViewById(R.id.list_title);
			holder.author = view.findViewById(R.id.list_author);
			holder.version = view.findViewById(R.id.list_version);
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
