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

package javax.microedition.lcdui;

import android.content.Context;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import javax.microedition.lcdui.event.CommandActionEvent;
import javax.microedition.lcdui.event.SimpleEvent;

public abstract class Item implements View.OnCreateContextMenuListener {
	public static final int PLAIN = 0;
	public static final int HYPERLINK = 1;
	public static final int BUTTON = 2;

	public static final int LAYOUT_DEFAULT = 0;
	public static final int LAYOUT_LEFT = 1;
	public static final int LAYOUT_RIGHT = 2;
	public static final int LAYOUT_CENTER = 3;
	public static final int LAYOUT_TOP = 16;
	public static final int LAYOUT_BOTTOM = 32;
	public static final int LAYOUT_VCENTER = 48;
	public static final int LAYOUT_NEWLINE_BEFORE = 256;
	public static final int LAYOUT_NEWLINE_AFTER = 512;
	public static final int LAYOUT_SHRINK = 1024;
	public static final int LAYOUT_EXPAND = 2048;
	public static final int LAYOUT_VSHRINK = 4096;
	public static final int LAYOUT_VEXPAND = 8192;
	public static final int LAYOUT_2 = 16384;

	private static final int HORIZONTAL_GRAVITY_MASK = 3;
	private static final int VERTICAL_GRAVITY_MASK = 48;

	private static final int LABEL_NO_ACTION = 0;
	private static final int LABEL_SHOW = 1;
	private static final int LABEL_HIDE = 2;

	private LinearLayout layout;
	private View contentview;

	private String label;
	private TextView labelview;
	private int labelmode;
	private int preferredWidth, preferredHeight;
	private int layoutmode;

	private Form owner;

	private ArrayList<Command> commands = new ArrayList<>();
	private ItemCommandListener listener = null;
	private Command defaultCommand;

	private SimpleEvent msgSetContextMenuListener = new SimpleEvent() {
		@Override
		public void process() {
			if (listener != null) {
				contentview.setOnCreateContextMenuListener(Item.this);
			} else {
				contentview.setLongClickable(false);
			}
		}
	};

	private SimpleEvent msgSetLabel = new SimpleEvent() {
		@Override
		public void process() {
			labelview.setText(label);

			switch (labelmode) {
				case LABEL_SHOW:
					layout.addView(labelview, 0);
					break;

				case LABEL_HIDE:
					layout.removeView(labelview);
					break;
			}

			labelmode = LABEL_NO_ACTION;
		}
	};

	public Item() {
		setLayout(LAYOUT_DEFAULT);
	}

	public void setLabel(String value) {
		if (layout != null) {
			if (label == null && value != null) {
				labelmode = LABEL_SHOW;
			} else if (label != null && value == null) {
				labelmode = LABEL_HIDE;
			}

			label = value;

			ViewHandler.postEvent(msgSetLabel);
		} else {
			label = value;
		}
	}

	public String getLabel() {
		return label;
	}

	public void setOwnerForm(Form form) {
		owner = form;
		clearItemView();
	}

	public Form getOwnerForm() {
		if (owner == null) {
			throw new IllegalStateException("call setOwnerForm() before calling getOwnerForm()");
		}

		return owner;
	}

	public boolean hasOwnerForm() {
		return owner != null;
	}

	public void notifyStateChanged() {
		if (owner != null) {
			owner.notifyItemStateChanged(this);
		}
	}

	public void setLayout(int value) {
		layoutmode = value;
	}

	public int getLayout() {
		return layoutmode;
	}

	/**
	 * Get the whole item
	 *
	 * @return LinearLayout with a label in the first row and some content in the second row
	 */
	public View getItemView() {
		if (layout == null) {
			Context context = owner.getParentActivity();

			layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);

			labelview = new TextView(context);
			labelview.setTextAppearance(context, android.R.style.TextAppearance_Medium);
			labelview.setText(label);

			if (label != null) {
				layout.addView(labelview, getLayoutParams());
			}

			contentview = getItemContentView();
			layout.addView(contentview, getLayoutParams());

			ViewHandler.postEvent(msgSetContextMenuListener);
		}

		return layout;
	}

	private LinearLayout.LayoutParams getLayoutParams() {
		int hwrap = LayoutParams.MATCH_PARENT;
		int vwrap = LayoutParams.WRAP_CONTENT;
		int gravity = Gravity.LEFT;

		if ((layoutmode & LAYOUT_SHRINK) != 0) {
			hwrap = LayoutParams.WRAP_CONTENT;
		} else if ((layoutmode & LAYOUT_EXPAND) != 0) {
			hwrap = LayoutParams.MATCH_PARENT;
		}

		if ((layoutmode & LAYOUT_VSHRINK) != 0) {
			vwrap = LayoutParams.WRAP_CONTENT;
		} else if ((layoutmode & LAYOUT_VEXPAND) != 0) {
			vwrap = LayoutParams.MATCH_PARENT;
		}

		int horizontal = layoutmode & HORIZONTAL_GRAVITY_MASK;
		if (horizontal == LAYOUT_CENTER) {
			gravity = Gravity.CENTER_HORIZONTAL;
		} else if (horizontal == LAYOUT_RIGHT) {
			gravity = Gravity.RIGHT;
			hwrap = LayoutParams.WRAP_CONTENT;
		} else if (horizontal == LAYOUT_LEFT) {
			gravity = Gravity.LEFT;
			hwrap = LayoutParams.WRAP_CONTENT;
		}

		int vertical = layoutmode & VERTICAL_GRAVITY_MASK;
		if (vertical == LAYOUT_VCENTER) {
			gravity |= Gravity.CENTER_VERTICAL;
		} else if (vertical == LAYOUT_BOTTOM) {
			gravity |= Gravity.BOTTOM;
			vwrap = LayoutParams.WRAP_CONTENT;
		} else if (vertical == LAYOUT_TOP) {
			gravity |= Gravity.TOP;
			vwrap = LayoutParams.WRAP_CONTENT;
		}

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(hwrap, vwrap);
		layoutParams.gravity = gravity;
		return layoutParams;
	}

	public void clearItemView() {
		layout = null;
		labelview = null;
		contentview = null;

		clearItemContentView();
	}

	/**
	 * Get the item content
	 */
	protected abstract View getItemContentView();

	protected abstract void clearItemContentView();

	public void addCommand(Command cmd) {
		if (cmd == null) {
			throw new NullPointerException();
		}
		if (!commands.contains(cmd)) {
			commands.add(cmd);
		}
	}

	public void removeCommand(Command cmd) {
		commands.remove(cmd);
		if (defaultCommand == cmd) {
			defaultCommand = null;
		}
	}

	public void setDefaultCommand(Command cmd) {
		defaultCommand = cmd;
		if (cmd == null) {
			return;
		}
		commands.remove(cmd);
		commands.add(0, cmd);
	}

	public void setItemCommandListener(ItemCommandListener listener) {
		this.listener = listener;

		if (layout != null) {
			ViewHandler.postEvent(msgSetContextMenuListener);
		}
	}

	public void setPreferredSize(int width, int height) {
		preferredWidth = width;
		preferredHeight = height;
	}

	public int getPreferredWidth() {
		return preferredWidth;
	}

	public int getPreferredHeight() {
		return preferredHeight;
	}

	public int getMinimumHeight() {
		return 0;
	}

	public int getMinimumWidth() {
		return 0;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.clear();

		for (Command cmd : commands) {
			menu.add(hashCode(), cmd.hashCode(), cmd.getPriority(), cmd.getAndroidLabel());
		}
	}

	public boolean contextMenuItemSelected(MenuItem item) {
		if (listener == null) {
			return false;
		}

		int id = item.getItemId();

		for (Command cmd : commands) {
			if (cmd.hashCode() == id) {
				if (owner != null) {
					owner.postEvent(CommandActionEvent.getInstance(listener, cmd, this));
				}
				return true;
			}
		}
		return false;
	}

	public void fireDefaultCommandAction() {
		if (defaultCommand != null) {
			owner.postEvent(CommandActionEvent.getInstance(listener, defaultCommand, this));
		}
	}
}