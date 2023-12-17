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
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;

import javax.microedition.lcdui.event.SimpleEvent;
import javax.microedition.util.ContextHolder;

public class Alert extends Screen implements DialogInterface.OnClickListener {
	public static final int FOREVER = -2;
	public static final Command DISMISS_COMMAND = new Command("", Command.OK, 0);
	private static final AlertCommandListener DEFAULT_LISTENER = new AlertCommandListener();

	private static class AlertCommandListener implements CommandListener {
		@Override
		public void commandAction(Command c, Displayable d) {
			((Alert) d).dismiss();
		}
	}

	private String text;
	private Image image;
	private AlertType type;
	private int timeout;
	private Gauge indicator;
	private AlertDialog alertDialog;
	private Displayable nextDisplayable;

	private Command[] commands;
	private int positive, negative, neutral;

	private final SimpleEvent msgSetString = new SimpleEvent() {
		@Override
		public void process() {
			alertDialog.setMessage(text);
		}
	};

	private final SimpleEvent msgSetImage = new SimpleEvent() {
		@Override
		public void process() {
			BitmapDrawable bitmapDrawable = new BitmapDrawable(image.getBitmap());
			alertDialog.setIcon(bitmapDrawable);
		}
	};

	private final SimpleEvent msgCommandsChanged = new SimpleEvent() {
		@Override
		public void process() {
			if (listener == DEFAULT_LISTENER) {
				alertDialog.setCancelable(true);
				alertDialog.setCanceledOnTouchOutside(true);
				return;
			}
			alertDialog.setCanceledOnTouchOutside(countCommands() == 1 && getCommands()[0] == DISMISS_COMMAND);
		}
	};

	public Alert(String title) {
		this(title, null, null, null);
	}

	public Alert(String title, String text, Image image, AlertType type) {
		super.addCommand(DISMISS_COMMAND);
		super.setTitle(title);

		this.text = text;
		this.image = image;
		this.type = type;
		this.timeout = FOREVER;

		setCommandListener(DEFAULT_LISTENER);
	}

	public void setType(AlertType type) {
		this.type = type;
	}

	public AlertType getType() {
		return type;
	}

	public void setString(String str) {
		text = str;

		if (alertDialog != null) {
			ViewHandler.postEvent(msgSetString);
		}
	}

	public String getString() {
		return text;
	}

	public void setImage(Image img) {
		image = img;

		if (alertDialog != null) {
			ViewHandler.postEvent(msgSetImage);
		}
	}

	public Image getImage() {
		return image;
	}

	public void setIndicator(Gauge indicator) {
		if (indicator == null) {
			if (this.indicator != null) {
				this.indicator.setAlert(null);
			}
		} else {
			if (indicator.isInteractive()) throw new IllegalArgumentException();
			indicator.setAlert(this);
		}
		this.indicator = indicator;
	}

	public Gauge getIndicator() {
		return indicator;
	}

	public int getDefaultTimeout() {
		return FOREVER;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getTimeout() {
		return timeout;
	}

	public boolean finiteTimeout() {
		return timeout > 0 && countCommands() == 1 && getCommands()[0] == DISMISS_COMMAND;
	}

	public AlertDialog prepareDialog() {
		Context context = ContextHolder.getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setTitle(getTitle());
		builder.setMessage(getString());
		builder.setOnDismissListener(dialog -> {
			if (countCommands() == 1 && getCommands()[0] == DISMISS_COMMAND && listener != null) {
				fireCommandAction(DISMISS_COMMAND);
			}
		});

		if (image != null) {
			builder.setIcon(new BitmapDrawable(context.getResources(), image.getBitmap()));
		}

		if (indicator != null) {
			builder.setView(indicator.getItemContentView());
		}

		commands = getCommands();
		Arrays.sort(commands);

		positive = -1;
		negative = -1;
		neutral = -1;

		for (int i = 0; i < commands.length; i++) {
			int cmdtype = commands[i].getCommandType();

			if (positive < 0 && cmdtype == Command.OK) {
				positive = i;
			} else if (negative < 0 && cmdtype == Command.CANCEL) {
				negative = i;
			} else if (neutral < 0) {
				neutral = i;
			}
		}
		for (int i = 0; i < commands.length; i++) {
			if (positive < 0 && negative != i && neutral != i) {
				positive = i;
			} else if (negative < 0 && positive != i && neutral != i) {
				negative = i;
			}
		}

		if (positive >= 0) {
			builder.setPositiveButton(commands[positive].getAndroidLabel(), this);
		}

		if (negative >= 0) {
			builder.setNegativeButton(commands[negative].getAndroidLabel(), this);
		}

		if (neutral >= 0) {
			builder.setNeutralButton(commands[neutral].getAndroidLabel(), this);
		}

		alertDialog = builder.create();
		if (listener == DEFAULT_LISTENER) {
			alertDialog.setCancelable(true);
			alertDialog.setCanceledOnTouchOutside(true);
		} else {
			alertDialog.setCanceledOnTouchOutside(commands.length == 1 && commands[0] == DISMISS_COMMAND);
		}
		return alertDialog;
	}

	@Override
	public void addCommand(Command cmd) {
		if (cmd != DISMISS_COMMAND) {
			super.addCommand(cmd);
			super.removeCommand(DISMISS_COMMAND);
			if (alertDialog != null) {
				ViewHandler.postEvent(msgCommandsChanged);
			}
		}
	}

	@Override
	public void removeCommand(Command cmd) {
		if (cmd != DISMISS_COMMAND) {
			super.removeCommand(cmd);
			if (countCommands() == 0) {
				if (alertDialog != null) {
					ViewHandler.postEvent(msgCommandsChanged);
				}
				super.addCommand(DISMISS_COMMAND);
			}
		}
	}

	@Override
	public void setCommandListener(CommandListener listener) {
		if (listener == null) {
			listener = DEFAULT_LISTENER;
		}
		super.setCommandListener(listener);
		if (alertDialog != null) {
			ViewHandler.postEvent(msgCommandsChanged);
		}
	}

	@Override
	public View getDisplayableView() {
		return null;
	}

	@Override
	public void clearDisplayableView() {
		if (alertDialog != null) {
			alertDialog.dismiss();
		}
	}

	@Override
	public View getScreenView() {
		return null;
	}

	@Override
	public void clearScreenView() {
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				fireCommandAction(commands[positive]);
				break;

			case DialogInterface.BUTTON_NEGATIVE:
				fireCommandAction(commands[negative]);
				break;

			case DialogInterface.BUTTON_NEUTRAL:
				fireCommandAction(commands[neutral]);
				break;
		}
	}

	void setReturnScreen(Displayable nextDisplayable) {
		if (nextDisplayable != null && nextDisplayable instanceof Alert) {
			throw new IllegalArgumentException("Alert cannot return to Alert");
		}
		this.nextDisplayable = nextDisplayable;
	}

	void dismiss() {
		if (alertDialog == null) {
			return;
		}
		Display.getDisplay(null).setCurrent(nextDisplayable);
		nextDisplayable = null;
		alertDialog = null;
	}
}