/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2018 Nikita Shakarun
 * Copyright 2019-2023 Yury Kharchenko
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

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import javax.microedition.lcdui.Choice;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;

public class CompoundListAdapter extends CompoundAdapter implements ListAdapter {
	private static int highlightColor;

	private final int listType;
	private final int viewResourceID;

	public CompoundListAdapter(int type, ArrayList<CompoundItem> items) {
		super(items);
		switch (type) {
			case Choice.IMPLICIT:
				viewResourceID = android.R.layout.simple_list_item_1;
				if (highlightColor == 0) {
					TypedValue typedValue = new TypedValue();
					Context context = ContextHolder.getActivity();
					context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorControlHighlight, typedValue, true);
					highlightColor = ContextCompat.getColor(context, typedValue.resourceId);
				}
				break;
			case Choice.EXCLUSIVE:
				viewResourceID = android.R.layout.simple_list_item_single_choice;
				break;
			case Choice.MULTIPLE:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					viewResourceID = android.R.layout.simple_list_item_multiple_choice;
				} else {
					// use a local copy from SDK 16 because older versions look different
					viewResourceID = R.layout.simple_list_item_multiple_choice;
				}
				break;
			default:
				throw new IllegalArgumentException("list type " + type + " is not supported");
		}
		listType = type;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = getView(position, convertView, parent, viewResourceID);

		boolean selected = getItem(position).isSelected();
		if (listType == Choice.IMPLICIT) {
			convertView.setBackgroundColor(selected ? highlightColor : Color.TRANSPARENT);
		} else {
			((CheckedTextView) convertView).setChecked(selected);
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
