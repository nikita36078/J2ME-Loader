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
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import javax.microedition.lcdui.event.CommandActionEvent;
import javax.microedition.lcdui.event.Event;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import androidx.appcompat.app.AppCompatActivity;

public abstract class Displayable {
	private MicroActivity parent;
	private String title;

	private ArrayList<Command> commands;
	protected CommandListener listener;

	private int tickermode;
	private Ticker ticker;
	private LinearLayout layout;
	private TextView marquee;

	private static EventQueue queue;
	protected static int virtualWidth;
	protected static int virtualHeight;

	private static final int TICKER_NO_ACTION = 0;
	private static final int TICKER_SHOW = 1;
	private static final int TICKER_HIDE = 2;

	private SimpleEvent msgSetTicker = new SimpleEvent() {
		@Override
		public void process() {
			if (ticker != null) {
				marquee.setText(ticker.getString());
			}
			switch (tickermode) {
				case TICKER_SHOW:
					layout.addView(marquee, 0);
					break;
				case TICKER_HIDE:
					layout.removeView(marquee);
					break;
			}
			tickermode = TICKER_NO_ACTION;
		}
	};

	static {
		queue = new EventQueue();
		queue.startProcessing();
	}

	public Displayable() {
		commands = new ArrayList<>();
		listener = null;
	}

	public static void setVirtualSize(int virtualWidth, int virtualHeight) {
		Displayable.virtualWidth = virtualWidth;
		Displayable.virtualHeight = virtualHeight;
	}

	public void setParentActivity(MicroActivity activity) {
		parent = activity;
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
			parent.runOnUiThread(() -> parent.getSupportActionBar().setTitle(Displayable.this.title));
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

	public View getDisplayableView() {
		if (layout == null) {
			Context context = getParentActivity();

			layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

			marquee = new TextView(context);
			marquee.setTextAppearance(context, android.R.style.TextAppearance_Medium);
			marquee.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			marquee.setSelected(true);
			marquee.setSingleLine();

			if (ticker != null) {
				marquee.setText(ticker.getString());
				layout.addView(marquee);
			}
		}

		return layout;
	}

	public void clearDisplayableView() {
		layout = null;
		marquee = null;
	}

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

	public EventQueue getEventQueue() {
		return queue;
	}

	public void postEvent(Event event) {
		queue.postEvent(event);
	}

	public int getWidth() {
		return virtualWidth;
	}

	public int getHeight() {
		return virtualHeight;
	}

	public void setTicker(Ticker newticker) {
		if (layout != null) {
			if (ticker == null && newticker != null) {
				tickermode = TICKER_SHOW;
			} else if (ticker != null && newticker == null) {
				tickermode = TICKER_HIDE;
			}

			ticker = newticker;

			ViewHandler.postEvent(msgSetTicker);
		} else {
			ticker = newticker;
		}
	}

	public Ticker getTicker() {
		return ticker;
	}

	public void sizeChanged(int w, int h) {
	}

	public boolean menuItemSelected(int id) {
		if (listener == null) {
			return true;
		}

		Command[] array = commands.toArray(new Command[0]);
		for (Command cmd : array) {
			if (cmd.hashCode() == id) {
				postEvent(CommandActionEvent.getInstance(listener, cmd, this));
			}
		}
		return true;
	}
}
