/*
 * Copyright 2012 Kulikov Dmitriy
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

package javax.microedition.lcdui.list;

import android.R.layout;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;

import javax.microedition.lcdui.Choice;

public class CompoundListAdapter extends CompoundAdapter implements ListAdapter {
	protected int listType;
	protected int viewResourceID;

	public CompoundListAdapter(int type) {
		this.listType = type;

		switch (type) {
			case Choice.IMPLICIT:
				viewResourceID = layout.simple_list_item_1;
				break;

			case Choice.EXCLUSIVE:
				viewResourceID = layout.simple_list_item_single_choice;
				break;

			case Choice.MULTIPLE:
				viewResourceID = layout.simple_list_item_multiple_choice;
				break;

			default:
				throw new IllegalArgumentException("list type " + type + " is not supported");
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = getView(position, convertView, parent, viewResourceID, true);

		boolean selected = getItem(position).isSelected();
		if (listType != Choice.IMPLICIT) {
			((CheckedTextView) convertView).setChecked(selected);
		} else if (selected) {
			convertView.setBackgroundColor(0x999999FF);
		} else {
			convertView.setBackgroundColor(Color.TRANSPARENT);
		}

		return convertView;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}
}