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
import java.util.Arrays;
import java.util.Collections;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.lcdui.list.CompoundListAdapter;
import javax.microedition.lcdui.list.ItemSelector;

public class List extends Screen implements Choice, ItemSelector, View.OnCreateContextMenuListener {
	public static final Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);

	private ArrayList<String> strings = new ArrayList<>();
	private ArrayList<Image> images = new ArrayList<>();
	private final ArrayList<Boolean> selected = new ArrayList<>();

	private ListView list;
	private CompoundListAdapter adapter;

	private int listType;
	private int selectedIndex = -1;
	private int fitPolicy;

	private Command selectCommand = SELECT_COMMAND;

	private SimpleEvent msgSetSelection = new SimpleEvent() {
		@Override
		public void process() {
			list.setSelection(selectedIndex);
		}
	};

	private SimpleEvent msgSetContextMenuListener = new SimpleEvent() {
		@Override
		public void process() {
			if (listener != null) {
				list.setOnCreateContextMenuListener(List.this);
			} else {
				list.setLongClickable(false);
			}
		}
	};

	private class ClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			selectedIndex = position;
			switch (listType) {
				case IMPLICIT:
					fireCommandAction(selectCommand, List.this);
					break;
				case EXCLUSIVE:
					if (position >= 0 && position < selected.size()) {
						Collections.fill(selected, Boolean.FALSE);
						selected.set(position, Boolean.TRUE);
					}
					break;
				case MULTIPLE:
					if (position >= 0 && position < selected.size()) {
						selected.set(position, !selected.get(position));
					}
					break;
			}
			adapter.notifyDataSetChanged();
		}
	}

	private ClickListener clicklistener = new ClickListener();

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

		setTitle(title);
	}

	public List(String title, int listType, String[] stringElements, Image[] imageElements) {
		this(title, listType);

		if (stringElements != null && imageElements != null && imageElements.length != stringElements.length) {
			throw new IllegalArgumentException("string and image arrays have different length");
		}

		if (stringElements != null) {
			strings.addAll(Arrays.asList(stringElements));
		}

		if (imageElements != null) {
			images.addAll(Arrays.asList(imageElements));
		}

		int size = Math.max(strings.size(), images.size());

		if (size > 0) {
			selected.addAll(Collections.nCopies(size, Boolean.FALSE));

			if (strings.size() == 0) {
				strings.addAll(Collections.nCopies(size, null));
			}

			if (images.size() == 0) {
				images.addAll(Collections.nCopies(size, null));
			}
		}
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
	public int append(String stringPart, Image imagePart) {
		synchronized (selected) {
			int index = selected.size();
			boolean select = index == 0 && listType != MULTIPLE;

			strings.add(stringPart);
			images.add(imagePart);
			selected.add(select);

			if (select) {
				selectedIndex = index;
			}

			if (list != null) {
				adapter.append(stringPart, imagePart);
			}

			return index;
		}
	}

	@Override
	public void delete(int elementNum) {
		synchronized (selected) {
			strings.remove(elementNum);
			images.remove(elementNum);
			selected.remove(elementNum);

			if (selected.size() == 0) {
				selectedIndex = -1;
			}

			if (list != null) {
				adapter.delete(elementNum);
			}
		}
	}

	@Override
	public void deleteAll() {
		synchronized (selected) {
			strings.clear();
			images.clear();
			selected.clear();

			selectedIndex = -1;

			if (list != null) {
				adapter.deleteAll();
			}
		}
	}

	@Override
	public Image getImage(int elementNum) {
		return images.get(elementNum);
	}

	@Override
	public int getSelectedFlags(boolean[] selectedArray) {
		synchronized (selected) {
			if (selectedArray.length < selected.size()) {
				throw new IllegalArgumentException("return array is too short");
			}

			int index = 0;
			int selectedCount = 0;

			for (Boolean flag : selected) {
				if (flag) {
					selectedCount++;
				}

				selectedArray[index++] = flag;
			}

			while (index < selectedArray.length) {
				selectedArray[index++] = false;
			}

			return selectedCount;
		}
	}

	@Override
	public int getSelectedIndex() {
		return selectedIndex;
	}

	@Override
	public String getString(int elementNum) {
		return strings.get(elementNum);
	}

	@Override
	public void insert(int elementNum, String stringPart, Image imagePart) {
		synchronized (selected) {
			boolean select = selected.size() == 0 && listType != MULTIPLE;

			strings.add(elementNum, stringPart);
			images.add(elementNum, imagePart);
			selected.add(elementNum, select);

			if (select) {
				selectedIndex = elementNum;
			}

			if (list != null) {
				adapter.insert(elementNum, stringPart, imagePart);
			}
		}
	}

	@Override
	public boolean isSelected(int elementNum) {
		synchronized (selected) {
			return selected.get(elementNum);
		}
	}

	@Override
	public void set(int elementNum, String stringPart, Image imagePart) {
		synchronized (selected) {
			strings.set(elementNum, stringPart);
			images.set(elementNum, imagePart);

			if (list != null) {
				adapter.set(elementNum, stringPart, imagePart);
			}
		}
	}

	@Override
	public void setSelectedFlags(boolean[] selectedArray) {
		if (listType == EXCLUSIVE || listType == IMPLICIT) {
			for (int i = 0; i < selectedArray.length; i++) {
				if (selectedArray[i]) {
					setSelectedIndex(i, true);
					return;
				}
			}
		}

		synchronized (selected) {
			if (selectedArray.length < selected.size()) {
				throw new IllegalArgumentException("array is too short");
			}

			int size = selected.size();

			for (int i = 0; i < size; i++) {
				selected.set(i, selectedArray[i]);
			}
		}
	}

	@Override
	public void setSelectedIndex(int elementNum, boolean flag) {
		synchronized (selected) {
			selected.set(elementNum, flag);

			if (flag) {
				selectedIndex = elementNum;
			}

			if (list != null) {
				if (flag) {
					ViewHandler.postEvent(msgSetSelection);
				}
			}
		}
	}

	@Override
	public void setFont(int elementNum, Font font) {
	}

	@Override
	public Font getFont(int elementNum) {
		return Font.getDefaultFont();
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
	public int size() {
		synchronized (selected) {
			return selected.size();
		}
	}

	@Override
	public View getScreenView() {
		Context context = getParentActivity();

		adapter = new CompoundListAdapter(context, this, listType);

		list = new ListView(context);
		list.setAdapter(adapter);

		int size = selected.size();

		for (int i = 0; i < size; i++) {
			adapter.append(strings.get(i), images.get(i));
		}

		if (listType == IMPLICIT && selectedIndex >= 0 && selectedIndex < selected.size()) {
			list.setSelection(selectedIndex);
		}

		list.setOnItemClickListener(clicklistener);
		ViewHandler.postEvent(msgSetContextMenuListener);

		return list;
	}

	@Override
	public void clearScreenView() {
		list = null;
		adapter = null;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
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
}