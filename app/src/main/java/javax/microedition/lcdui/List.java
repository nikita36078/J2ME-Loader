/*
 * Copyright 2012 Kulikov Dmitriy
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
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.lcdui.list.CompoundItem;
import javax.microedition.lcdui.list.CompoundListAdapter;

public class List extends Screen implements Choice {
	public static final Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);

	private ListView list;
	private final CompoundListAdapter adapter;

	private final int listType;
	private int selectedIndex = -1;
	private int fitPolicy;

	private Command selectCommand = SELECT_COMMAND;

	private final SimpleEvent msgSetContextMenuListener = new SimpleEvent() {
		@Override
		public void process() {
			if (listener != null) {
				list.setOnCreateContextMenuListener(List.this::onCreateContextMenu);
			} else {
				list.setLongClickable(false);
			}
		}
	};

	private int mSize;

	public List(String title, int listType) {
		switch (listType) {
			case IMPLICIT:
			case EXCLUSIVE:
			case MULTIPLE:
				this.listType = listType;
				break;
			default:
				throw new IllegalArgumentException("list type " + listType + " is not supported");
		}
		adapter = new CompoundListAdapter(listType);
		setTitle(title);
	}

	public List(String title, int listType, String[] stringElements, Image[] imageElements) {
		this(title, listType);
		if (stringElements == null) {
			throw new NullPointerException("String elements array is NULL");
		}
		int size = stringElements.length;
		for (int i = 0; i < size; i++) {
			String s = stringElements[i];
			if (s == null) {
				throw new NullPointerException("String element [" + i + "] is NULL");
			}
		}
		if (imageElements != null && imageElements.length != size) {
			throw new IllegalArgumentException("String and image arrays have different length");
		}
		ArrayList<CompoundItem> items = new ArrayList<>(size);
		if (imageElements != null) {
			for (int i = 0; i < size; i++) {
				items.add(new CompoundItem(stringElements[i], imageElements[i]));
			}
		} else {
			for (String stringElement : stringElements) {
				items.add(new CompoundItem(stringElement));
			}
		}
		adapter.setAll(items);
		mSize = size;
	}

	public void setSelectCommand(Command cmd) {
		if (selectCommand != SELECT_COMMAND) {
			removeCommand(selectCommand);
		}

		if (cmd != null) {
			addCommand(selectCommand = cmd);
		} else {
			selectCommand = SELECT_COMMAND;
		}
	}

	@Override
	public synchronized int append(String stringPart, Image imagePart) {
		int index = mSize;
		insert(index, stringPart, imagePart);
		return index;
	}

	@Override
	public synchronized void delete(int elementNum) {
		if (elementNum < 0 || elementNum >= mSize) {
			throw new IndexOutOfBoundsException("elementNum = " + elementNum + ", but size = " + mSize);
		}
		if (--mSize == 0) {
			selectedIndex = -1;
		}
		adapter.delete(elementNum);
	}

	@Override
	public synchronized void deleteAll() {
		mSize = 0;
		selectedIndex = -1;
		adapter.deleteAll();
	}

	@Override
	public synchronized Image getImage(int elementNum) {
		if (elementNum < 0 || elementNum >= mSize) {
			throw new IndexOutOfBoundsException("elementNum = " + elementNum + ", but size = " + mSize);
		}
		return adapter.getItem(elementNum).getImage();
	}

	@Override
	public synchronized int getSelectedFlags(boolean[] selectedArray) {
		int size = mSize;
		if (selectedArray.length < size) {
			throw new IllegalArgumentException("return array is too short");
		}

		int index = 0;
		int selectedCount = 0;

		while (index < size) {
			boolean flag = adapter.getItem(index).isSelected();
			selectedArray[index++] = flag;
			if (flag) {
				selectedCount++;
			}
		}

		while (index < selectedArray.length) {
			selectedArray[index++] = false;
		}

		return selectedCount;
	}

	@Override
	public synchronized int getSelectedIndex() {
		return selectedIndex;
	}

	@Override
	public synchronized String getString(int elementNum) {
		if (elementNum < 0 || elementNum >= mSize) {
			throw new IndexOutOfBoundsException("elementNum = " + elementNum + ", but size = " + mSize);
		}
		return adapter.getItem(elementNum).getString();
	}

	@Override
	public synchronized void insert(int elementNum, String stringPart, Image imagePart) {
		if (elementNum < 0 || elementNum > mSize) {
			throw new IndexOutOfBoundsException("elementNum = " + elementNum + ", but size = " + mSize);
		}
		CompoundItem item = new CompoundItem(stringPart, imagePart);
		boolean select = mSize == 0 && listType != MULTIPLE;

		if (select) {
			selectedIndex = elementNum;
			item.setSelected(true);
		}

		adapter.insert(elementNum, item, select);
		mSize++;
	}

	@Override
	public synchronized boolean isSelected(int elementNum) {
		if (elementNum < 0 || elementNum >= mSize) {
			throw new IndexOutOfBoundsException("elementNum = " + elementNum + ", but size = " + mSize);
		}
		return adapter.getItem(elementNum).isSelected();
	}

	@Override
	public synchronized void set(int elementNum, String stringPart, Image imagePart) {
		if (elementNum < 0 || elementNum >= mSize) {
			throw new IndexOutOfBoundsException("elementNum = " + elementNum + ", but size = " + mSize);
		}
		adapter.set(elementNum, stringPart, imagePart);
	}

	@Override
	public synchronized void setSelectedFlags(boolean[] selectedArray) {
		if (selectedArray.length < mSize) {
			throw new IllegalArgumentException("array is too short");
		}

		if (listType == EXCLUSIVE || listType == IMPLICIT) {
			for (int i = 0; i < selectedArray.length; i++) {
				if (selectedArray[i]) {
					setSelectedIndex(i, true);
					return;
				}
			}
		}
		adapter.setSelectionFlags(selectedArray);
	}

	@Override
	public synchronized void setSelectedIndex(int elementNum, boolean flag) {
		if (!flag && listType != MULTIPLE) return;
		selectedIndex = elementNum;
		if (listType == MULTIPLE) {
			adapter.setSelection(elementNum, flag);
		} else {
			adapter.setExclusiveSelection(elementNum);
		}
	}

	@Override
	public synchronized void setFont(int elementNum, Font font) {
		if (elementNum < 0 || elementNum >= mSize) {
			throw new IndexOutOfBoundsException("elementNum = " + elementNum + ", but size = " + mSize);
		}
		adapter.setFont(elementNum, font);
	}

	@Override
	public synchronized Font getFont(int elementNum) {
		return adapter.getItem(elementNum).getFont();
	}

	@Override
	public void setFitPolicy(int fitPolicy) {
		this.fitPolicy = fitPolicy;
	}

	@Override
	public int getFitPolicy() {
		return fitPolicy;
	}

	@Override
	public synchronized int size() {
		return mSize;
	}

	@Override
	public View getScreenView() {
		Context context = getParentActivity();

		list = new ListView(context);
		list.setAdapter(adapter);

		if (listType == IMPLICIT && selectedIndex >= 0 && selectedIndex < mSize) {
			list.setSelection(selectedIndex);
		}

		list.setOnItemClickListener(this::onItemClick);
		list.setOnItemLongClickListener(this::onItemLongClick);
		list.setOnCreateContextMenuListener(this::onCreateContextMenu);
		return list;
	}

	@Override
	public void clearScreenView() {
		list = null;
	}

	private void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.clear();

		for (Command cmd : getCommands()) {
			menu.add(hashCode(), cmd.hashCode(), cmd.getPriority(), cmd.getAndroidLabel());
		}
	}

	public boolean contextMenuItemSelected(MenuItem item, int selectedIndex) {
		if (listener == null) {
			return false;
		}
		this.selectedIndex = selectedIndex;

		int id = item.getItemId();

		for (Command cmd : getCommands()) {
			if (cmd.hashCode() == id) {
				fireCommandAction(cmd, this);
				return true;
			}
		}
		return false;
	}

	private void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		setSelectedIndex(position, true);
		if (listType == IMPLICIT) {
			fireCommandAction(selectCommand, List.this);
		}
	}

	private boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		setSelectedIndex(position, true);
		return getCommands().length == 0;
	}
}