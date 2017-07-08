package com.nokia.mid.ui;

import javax.microedition.lcdui.Command;

final class ButtonCommand extends Command {
	int key;

	ButtonCommand(int key, int type) {
		super("", type, 1);
		this.key = key;
	}

	public final String toString() {
		return super.toString() + "key=" + this.key;
	}
}
