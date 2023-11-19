/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2018 Nikita Shakarun
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

package javax.microedition.lcdui;

import android.content.Context;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;

import javax.microedition.util.ContextHolder;

public class Form extends Screen {
	private static final float BORDER_PADDING = 7;

	private final ArrayList<Item> items = new ArrayList<>();
	private ItemStateListener listener;

	private ScrollView scrollview;
	private LinearLayout layout;

	public Form(String title) {
		setTitle(title);
	}

	public Form(String title, Item[] elements) {
		setTitle(title);
		// null array is correct: create empty Form
		if (elements == null) {
			return;
		}
		for (int i = 0, elementsLength = elements.length; i < elementsLength; i++) {
			Item item = elements[i];
			if (item == null) {
				throw new NullPointerException("Item at index " + i + " is null");
			}
			if (item.hasOwner()) {
				throw new IllegalStateException("Item at index " + i + " is already owned by another container");
			}
		}
		for (Item item : elements) {
			items.add(item);
			item.setOwner(this);
		}
	}

	public Item get(int index) {
		return items.get(index);
	}

	public int size() {
		return items.size();
	}

	public int append(String text) {
		return append(new StringItem(null, text));
	}

	public int append(Image img) {
		return append(new ImageItem(null, img, ImageItem.LAYOUT_DEFAULT, null));
	}

	public int append(final Item item) {
		if (item.hasOwner()) {
			throw new IllegalStateException();
		}

		items.add(item);
		item.setOwner(this);
		if (layout != null) {
			ViewHandler.postEvent(() -> layout.addView(item.getItemView()));
		}
		return items.size() - 1;
	}

	public void insert(final int index, final Item item) {
		if (item.hasOwner()) {
			throw new IllegalStateException();
		}

		items.add(index, item);
		item.setOwner(this);
		if (layout != null) {
			ViewHandler.postEvent(() -> layout.addView(item.getItemView(), index));
		}
	}

	public void set(final int index, final Item item) {
		if (item.hasOwner()) {
			throw new IllegalStateException();
		}

		items.set(index, item).setOwner(null);
		item.setOwner(this);
		if (layout != null) {
			ViewHandler.postEvent(() -> {
				View v = item.getItemView();
				layout.removeViewAt(index);
				layout.addView(v, index);
			});
		}
	}

	public void delete(final int index) {
		items.remove(index).setOwner(null);

		if (layout != null) {
			ViewHandler.postEvent(() -> layout.removeViewAt(index));
		}
	}

	public void deleteAll() {
		for (Item item : items) {
			item.setOwner(null);
		}

		items.clear();

		if (layout != null) {
			ViewHandler.postEvent(() -> layout.removeAllViews());
		}
	}

	public void setItemStateListener(ItemStateListener listener) {
		this.listener = listener;
	}

	public void notifyItemStateChanged(Item item) {
		if (listener != null) {
			listener.itemStateChanged(item);
		}
	}

	@Override
	public View getScreenView() {
		if (scrollview == null) {
			Context context = ContextHolder.getActivity();

			layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);

			int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BORDER_PADDING, context.getResources().getDisplayMetrics()));
			layout.setPadding(padding, padding, padding, padding);

			scrollview = new ScrollView(context);
			scrollview.addView(layout);

			for (Item item : items) {
				layout.addView(item.getItemView());
			}
		}

		return scrollview;
	}

	@Override
	public void clearScreenView() {
		scrollview = null;
		layout = null;

		Item[] array = items.toArray(new Item[0]);
		for (Item item : array) {
			item.clearItemView();
		}
	}

	public void contextMenuItemSelected(MenuItem menuitem) {
		for (Item item : items) {
			if (menuitem.getGroupId() == item.hashCode() && item.contextMenuItemSelected(menuitem)) {
				return;
			}
		}

	}
}
