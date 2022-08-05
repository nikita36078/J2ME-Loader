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

import android.text.TextUtils;

import androidx.annotation.NonNull;

public class Command implements Comparable<Command> {
	public static final int SCREEN = 1;
	public static final int BACK = 2;
	public static final int CANCEL = 3;
	public static final int OK = 4;
	public static final int HELP = 5;
	public static final int STOP = 6;
	public static final int EXIT = 7;
	public static final int ITEM = 8;

	private final String shortLabel;
	private final String longLabel;
	private final int commandType;
	private final int priority;

	public Command(String label, int commandType, int priority) {
		this(label, null, commandType, priority);
	}

	public Command(String shortLabel, String longLabel, int commandType, int priority) {
		this.shortLabel = shortLabel;
		this.longLabel = longLabel;
		this.commandType = commandType;
		this.priority = priority;
	}

	public String getLabel() {
		return shortLabel;
	}

	public String getLongLabel() {
		return longLabel;
	}

	public String getAndroidLabel() {
		if (shortLabel.length() > 0) {
			return shortLabel;
		} else {
			String label;
			switch (commandType) {
				case SCREEN:
					label = "Screen";
					break;
				case BACK:
					label = "Back";
					break;
				case CANCEL:
					label = "Cancel";
					break;
				case OK:
					label = "OK";
					break;
				case HELP:
					label = "Help";
					break;
				case STOP:
					label = "Stop";
					break;
				case EXIT:
					label = "Exit";
					break;
				case ITEM:
					label = "Item";
					break;
				default:
					label = shortLabel;
					break;
			}
			return label;
		}
	}

	public int getCommandType() {
		return commandType;
	}

	public int getPriority() {
		return priority;
	}

	public int hashCode() {
		int hash = 3;

		hash = 97 * hash + (shortLabel != null ? shortLabel.hashCode() : 0);
		hash = 97 * hash + (longLabel != null ? longLabel.hashCode() : 0);
		hash = 97 * hash + commandType;
		hash = 97 * hash + priority;

		return hash;
	}

	@Override
	public int compareTo(Command cmd) {
		int p = commandType - cmd.commandType;
		if (p != 0) {
			return p;
		}
		return priority - cmd.priority;
	}

	@NonNull
	@Override
	public String toString() {
		return TextUtils.isEmpty(longLabel) ? getAndroidLabel() : longLabel;
	}
}