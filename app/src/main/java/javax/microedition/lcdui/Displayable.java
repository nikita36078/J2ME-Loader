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

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.ArrayList;

import javax.microedition.lcdui.event.CommandActionEvent;
import javax.microedition.lcdui.event.Event;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

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

	public AppCompatActivity getParentActivity() {
		if (parent == null) {
			return ContextHolder.getCurrentActivity();
		}
		return parent;
	}

	public void setTitle(String title) {
		this.title = title;

		if (parent != null) {
			parent.getSupportActionBar().setTitle(title);
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

	public CommandListener getCommandListener() {
		return listener;
	}

	public void setCommandListener(CommandListener listener) {
		this.listener = listener;
	}

	public void fireCommandAction(Command c, Displayable d) {
		if (listener != null) {
			queue.postEvent(CommandActionEvent.getInstance(listener, c, d));
		}
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
