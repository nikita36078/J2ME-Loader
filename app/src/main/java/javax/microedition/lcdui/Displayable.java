/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017 Nikita Shakarun
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

import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

import javax.microedition.lcdui.event.CommandActionEvent;
import javax.microedition.lcdui.event.Event;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.pointer.VirtualKeyboard;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import ua.naiksoftware.j2meloader.R;

public abstract class Displayable {
	private MicroActivity parent;
	private String title;

	private ArrayList<Command> commands;
	private CommandListener listener;

	private static EventQueue queue;

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

	private void switchLayoutEditMode(int mode) {
		if (this instanceof Canvas && ContextHolder.getVk() != null) {
			ContextHolder.getVk().switchLayoutEditMode(mode);
		}
	}

	public boolean menuItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (item.getGroupId() == R.id.action_group_common_settings) {
			if (id == R.id.action_exit_midlet) {
				parent.showExitConfirmation();
			} else if (this instanceof Canvas && ContextHolder.getVk() != null) {
				VirtualKeyboard vk = ContextHolder.getVk();
				switch (id) {
					case R.id.action_layout_edit_mode:
						vk.switchLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
						break;
					case R.id.action_layout_scale_mode:
						vk.switchLayoutEditMode(VirtualKeyboard.LAYOUT_SCALES);
						break;
					case R.id.action_layout_edit_finish:
						vk.switchLayoutEditMode(VirtualKeyboard.LAYOUT_EOF);
						break;
					case R.id.action_layout_switch:
						vk.switchLayout();
						break;
				}
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

	public int getWidth() {
		return ContextHolder.getDisplayWidth();
	}

	public int getHeight() {
		return ContextHolder.getDisplayHeight();
	}
}
