package com.nokia.mid.ui;

import javax.microedition.lcdui.Command;

final class ButtonCommand extends Command {
    int key;

    ButtonCommand(int i, int i2) {
        super("", i2, 1);
        this.key = i;
    }

    public final String toString() {
        return super.toString() + "key=" + this.key;
    }
}
