/*
 * Copyright 2012 Kulikov Dmitriy
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
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;

import java.util.ArrayList;

import javax.microedition.lcdui.Image;

public abstract class CompoundAdapter implements Adapter {
	protected ArrayList<CompoundItem> items;
	protected ArrayList<DataSetObserver> observers;
	protected Context context;

	public CompoundAdapter(Context context) {
		this.context = context;

		items = new ArrayList();
		observers = new ArrayList();
	}

	public void append(String stringPart, Image imagePart) {
		items.add(new CompoundItem(stringPart, imagePart));
		notifyDataSetChanged();
	}

	public void insert(int elementNum, String stringPart, Image imagePart) {
		items.add(elementNum, new CompoundItem(stringPart, imagePart));
		notifyDataSetChanged();
	}

	public void set(int elementNum, String stringPart, Image imagePart) {
		items.set(elementNum, new CompoundItem(stringPart, imagePart));
		notifyDataSetChanged();
	}

	public void delete(int elementNum) {
		items.remove(elementNum);
		notifyDataSetChanged();
	}

	public void deleteAll() {
		items.clear();
		notifyDataSetChanged();
	}

	public int getCount() {
		return items.size();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public CompoundItem getItem(int position) {
		return items.get(position);
	}

	public boolean hasStableIds() {
		return true;
	}

	public long getItemId(int position) {
		return position;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public int getItemViewType(int position) {
		return 0;
	}

	protected View getView(int position, View convertView, ViewGroup parent, int viewResourceID, boolean useImagePart) {
		TextView textview;

		if (convertView != null && convertView instanceof TextView) {
			textview = (TextView) convertView;
		} else {
			textview = (TextView) LayoutInflater.from(context).inflate(viewResourceID, null);
		}

		CompoundItem item = items.get(position);
		textview.setText(item.getString());

		if (useImagePart) {
			textview.setCompoundDrawablesWithIntrinsicBounds(item.getDrawable(), null, null, null);
			textview.setCompoundDrawablePadding(textview.getPaddingLeft());
		}

		return textview;
	}

	public abstract View getView(int position, View convertView, ViewGroup parent);

	public void registerDataSetObserver(DataSetObserver observer) {
		if (!observers.contains(observer)) {
			observers.add(observer);
		}
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		observers.remove(observer);
	}

	public void notifyDataSetChanged() {
		for (DataSetObserver observer : observers) {
			try {
				observer.onChanged();
			} catch (Exception e) {

			}
		}
	}

	public void notifyDataSetInvalidated() {
		for (DataSetObserver observer : observers) {
			observer.onInvalidated();
		}
	}
}