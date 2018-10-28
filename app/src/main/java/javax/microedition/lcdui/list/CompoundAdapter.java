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

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;

import java.util.ArrayList;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ViewHandler;
import javax.microedition.lcdui.event.SimpleEvent;

public abstract class CompoundAdapter implements Adapter {
	private ArrayList<CompoundItem> items;
	private ArrayList<DataSetObserver> observers;
	protected Context context;

	private SimpleEvent msgDataSetChanged = new SimpleEvent() {
		@Override
		public void process() {
			for (DataSetObserver observer : observers) {
				try {
					observer.onChanged();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	private SimpleEvent msgDataSetInvalidated = new SimpleEvent() {
		@Override
		public void process() {
			for (DataSetObserver observer : observers) {
				observer.onInvalidated();
			}
		}
	};

	public CompoundAdapter(Context context) {
		this.context = context;

		items = new ArrayList<>();
		observers = new ArrayList<>();
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

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return items.isEmpty();
	}

	@Override
	public CompoundItem getItem(int position) {
		return items.get(position);
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	protected View getView(int position, View convertView, ViewGroup parent, int viewResourceID, boolean useImagePart) {
		TextView textview;

		if (convertView instanceof TextView) {
			textview = (TextView) convertView;
		} else {
			textview = (TextView) LayoutInflater.from(context).inflate(viewResourceID, null);
		}

		CompoundItem item = items.get(position);
		textview.setText(item.getString());

		if (useImagePart) {
			textview.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
				textview.setCompoundDrawablesWithIntrinsicBounds(item.getDrawable(textview.getHeight()),
						null, null, null);
				textview.setCompoundDrawablePadding(textview.getPaddingLeft());
			});
		}

		return textview;
	}

	@Override
	public abstract View getView(int position, View convertView, ViewGroup parent);

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		if (!observers.contains(observer)) {
			observers.add(observer);
		}
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		observers.remove(observer);
	}

	public void notifyDataSetChanged() {
		ViewHandler.postEvent(msgDataSetChanged);
	}

	public void notifyDataSetInvalidated() {
		ViewHandler.postEvent(msgDataSetInvalidated);
	}
}