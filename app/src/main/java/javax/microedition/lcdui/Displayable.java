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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import java.util.ArrayList;

import javax.microedition.lcdui.event.CommandActionEvent;
import javax.microedition.lcdui.event.Event;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import ua.naiksoftware.j2meloader.R;

public abstract class Displayable {
	private MicroActivity parent;
	private String title;

	private ArrayList<Command> commands;
	private CommandListener listener;

	private static EventQueue queue;
	private static final int ADDITIONAL_MENU_SIZE = 5;
	private static final int MENU_EXIT = 1;
	private static final int MENU_KEY_EDIT = 2;
	private static final int MENU_KEY_SCALE = 3;
	private static final int MENU_KEY_FINISH = 4;

	static {
		queue = new EventQueue();
		queue.startProcessing();
	}

	public Displayable() {
		commands = new ArrayList();
		listener = null;
	}

	public void setParentActivity(MicroActivity activity) {
		parent = activity;

		clearDisplayableView();
	}

	public MicroActivity getParentActivity() {
		if (parent == null) {
			return ContextHolder.getCurrentActivity();
		}
		return parent;
	}

	public void setTitle(String title) {
		this.title = title;

		if (parent != null) {
			parent.setTitle(title);
		}
	}

	public String getTitle() {
		return title;
	}

	public boolean isShown() {
		if (parent != null) {
			return parent.isVisible() && parent.getCurrent() == this;
		}

		return false;
	}

	public abstract View getDisplayableView();

	public abstract void clearDisplayableView();

	public void addCommand(Command cmd) {
		commands.add(cmd);
	}

	public void removeCommand(Command cmd) {
		commands.remove(cmd);
	}

	public void removeAllCommands() {
		commands.clear();
	}

	public int countCommands() {
		return commands.size();
	}

	public Command[] getCommands() {
		return commands.toArray(new Command[0]);
	}

	public void setCommandListener(CommandListener listener) {
		this.listener = listener;
	}

	public void fireCommandAction(Command c, Displayable d) {
		if (listener != null) {
			queue.postEvent(CommandActionEvent.getInstance(listener, c, d));
		}
	}

	public void populateMenu(Menu menu) {
		menu.clear();
		SubMenu subMenu = menu.addSubMenu(Menu.NONE, 0, 0, R.string.common_settings);
		subMenu.add(Menu.NONE, MENU_EXIT, 0, R.string.exit);
		subMenu.add(Menu.NONE, MENU_KEY_EDIT, 0, R.string.layout_edit_mode);
		subMenu.add(Menu.NONE, MENU_KEY_SCALE, 0, R.string.layout_scale_mode);
		subMenu.add(Menu.NONE, MENU_KEY_FINISH, 0, R.string.layout_edit_finish);

		for (Command cmd : commands) {
			menu.add(Menu.NONE, cmd.hashCode(), cmd.getPriority(), cmd.getLabel());
		}
	}

	public void showExitConfirmation() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(parent);
		alertBuilder.setTitle(R.string.CONFIRMATION_REQUIRED)
				.setMessage(R.string.FORCE_CLOSE_CONFIRMATION)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface p1, int p2) {
						Runnable r = new Runnable() {
							public void run() {
								try {
									MIDlet.callDestroyApp(true);
								} catch (Throwable ex) {
									ex.printStackTrace();
								}
								ContextHolder.notifyDestroyed();
								System.exit(1);
							}
						};
						(new Thread(r)).start();
					}
				})
				.setNegativeButton(android.R.string.no, null);
		alertBuilder.create().show();
	}

	public boolean menuItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id < ADDITIONAL_MENU_SIZE && id > 0) {
			switch (id) {
				case MENU_EXIT:
					showExitConfirmation();
					break;
				case MENU_KEY_EDIT:
					ContextHolder.getVk().switchLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
					break;
				case MENU_KEY_SCALE:
					ContextHolder.getVk().switchLayoutEditMode(VirtualKeyboard.LAYOUT_SCALES);
					break;
				case MENU_KEY_FINISH:
					ContextHolder.getVk().switchLayoutEditMode(VirtualKeyboard.LAYOUT_EOF);
					break;
			}
			return true;
		}

		if (listener == null) {
			return false;
		}

		for (Command cmd : commands) {
			if (cmd.hashCode() == id) {
				queue.postEvent(CommandActionEvent.getInstance(listener, cmd, this));
				return true;
			}
		}
		return false;
	}

	public EventQueue getEventQueue() {
		return queue;
	}

	public void postEvent(Event event) {
		queue.postEvent(event);
	}

	// Added by Naik
	public int getWidth() {
		return ContextHolder.getDisplayWidth();
	}

	// Added by Naik
	public int getHeight() {
		return ContextHolder.getDisplayHeight();
	}
}
