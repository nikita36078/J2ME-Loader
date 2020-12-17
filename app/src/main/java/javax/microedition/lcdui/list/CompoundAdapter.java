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

import android.database.DataSetObserver;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;

public abstract class CompoundAdapter implements Adapter, Handler.Callback {

	private static final int NOTIFY_CHANGED = 1;
	private static final int NOTIFY_INVALIDATED = 2;
	private static final int APPEND = 3;
	private static final int ADD = 4;
	private static final int SET = 6;
	private static final int DELETE = 7;
	private static final int CLEAR = 8;
	private static final int SET_ALL = 9;
	private static final int SET_SELECTION_MULTIPLE = 10;
	private static final int SET_SELECTION = 11;
	private static final int SET_FONT = 12;
	private static final int SET_EXCLUSIVE_SELECTION = 13;
	private static final int APPEND_AND_SELECT = 14;

	private final Handler mHandler = new Handler(Looper.getMainLooper(), this);
	private final ArrayList<CompoundItem> items = new ArrayList<>();
	private final ArrayList<DataSetObserver> observers = new ArrayList<>();

	public void add(String stringPart, Image imagePart) {
		mHandler.obtainMessage(APPEND, new CompoundItem(stringPart, imagePart)).sendToTarget();
	}

	public void insert(int elementNum, String stringPart, Image imagePart) {
		mHandler.obtainMessage(ADD, elementNum, 0, new CompoundItem(stringPart, imagePart)).sendToTarget();
	}

	public void set(int elementNum, String stringPart, Image imagePart) {
		mHandler.obtainMessage(SET, elementNum, 0, new CompoundItem(stringPart, imagePart)).sendToTarget();
	}

	public void delete(int elementNum) {
		mHandler.obtainMessage(DELETE, elementNum, 0).sendToTarget();
	}

	public void deleteAll() {
		mHandler.obtainMessage(CLEAR).sendToTarget();
	}

	public void setAll(ArrayList<CompoundItem> items) {
		mHandler.obtainMessage(SET_ALL, items).sendToTarget();
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

	View getView(int position, View convertView, ViewGroup parent, int viewResourceID, boolean useImagePart) {
		TextView textview;

		if (convertView instanceof TextView) {
			textview = (TextView) convertView;
		} else {
			textview = (TextView) LayoutInflater.from(parent.getContext()).inflate(viewResourceID, null);
		}

		CompoundItem item = items.get(position);

		if (useImagePart && item.getImage() != null) {
			Paint.FontMetrics fm = textview.getPaint().getFontMetrics();
			float lineHeight = fm.leading + fm.bottom - fm.top;
			Drawable drawable = item.getDrawable(lineHeight);
			SpannableStringBuilder ssb = new SpannableStringBuilder(" ");
			ImageSpan imageSpan = new ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM);
			ssb.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.append(item.getString());
			textview.setText(ssb);
		} else {
			textview.setText(item.getString());
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

	public void add(CompoundItem item) {
		mHandler.obtainMessage(APPEND, item).sendToTarget();
	}

	public void insert(int index, CompoundItem item, boolean clearSelection) {
		mHandler.obtainMessage(ADD, index, clearSelection ? 1 : 0, item).sendToTarget();
	}

	public void setSelectionFlags(boolean[] selectedArray) {
		mHandler.obtainMessage(SET_SELECTION_MULTIPLE, selectedArray.clone()).sendToTarget();
	}

	public void setSelection(int index, boolean flag) {
		mHandler.obtainMessage(SET_SELECTION, index, flag ? 1 : 0).sendToTarget();
	}

	public void setExclusiveSelection(int index) {
		mHandler.obtainMessage(SET_EXCLUSIVE_SELECTION, index, 0).sendToTarget();
	}

	public void setFont(int index, Font font) {
		mHandler.obtainMessage(SET_FONT, index, 0, font).sendToTarget();
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case NOTIFY_CHANGED:
				break;
			case NOTIFY_INVALIDATED:
				for (DataSetObserver observer : observers) {
					observer.onInvalidated();
				}
				return true;
			case APPEND_AND_SELECT:
				for (CompoundItem item : items) {
					item.setSelected(false);
				}
			case APPEND:
				items.add((CompoundItem) msg.obj);
				break;
			case ADD:
				if (msg.arg2 == 1) {
					for (CompoundItem item : items) {
						item.setSelected(false);
					}
				}
				items.add(msg.arg1, (CompoundItem) msg.obj);
				break;
			case SET:
				CompoundItem item = (CompoundItem) msg.obj;
				items.set(msg.arg1, item);
				break;
			case DELETE:
				items.remove(msg.arg1);
				break;
			case CLEAR:
				items.clear();
				break;
			case SET_ALL:
				items.clear();
				//noinspection unchecked
				items.addAll((Collection<CompoundItem>) msg.obj);
				break;
			case SET_SELECTION_MULTIPLE:
				boolean[] flags = (boolean[]) msg.obj;
				for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
					items.get(i).setSelected(flags[i]);
				}
				break;
			case SET_SELECTION:
				items.get(msg.arg1).setSelected(msg.arg2 == 1);
				break;
			case SET_FONT:
				items.get(msg.arg1).setFont((Font) msg.obj);
				break;
			case SET_EXCLUSIVE_SELECTION:
				for (CompoundItem itm : items) {
					itm.setSelected(false);
				}
				items.get(msg.arg1).setSelected(true);
				break;
			default:
				return false;
		}
		for (DataSetObserver observer : observers) {
			try {
				observer.onChanged();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}