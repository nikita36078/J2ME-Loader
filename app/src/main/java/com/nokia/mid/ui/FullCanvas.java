package com.nokia.mid.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;

public abstract class FullCanvas extends Canvas {
    public static final int KEY_DOWN_ARROW = -2;
    public static final int KEY_END = -11;
    public static final int KEY_LEFT_ARROW = -3;
    public static final int KEY_RIGHT_ARROW = -4;
    public static final int KEY_SEND = -10;
    public static final int KEY_SOFTKEY1 = -6;
    public static final int KEY_SOFTKEY2 = -7;
    public static final int KEY_SOFTKEY3 = -5;
    public static final int KEY_UP_ARROW = -1;

    protected FullCanvas() {
        super.setFullScreenMode(true);
        super.addCommand(new ButtonCommand(KEY_SOFTKEY1,Command.OK));
        super.addCommand(new ButtonCommand(KEY_SOFTKEY2,Command.BACK));
        super.setCommandListener(new CommandHandler(this));
    }

    public void addCommand(Command command) {
        throw new IllegalStateException();
    }

    public int getHeight() {
        return super.getHeight();
    }

    public int getWidth() {
        return super.getWidth();
    }

    public void setCommandListener(CommandListener commandListener) {
        throw new IllegalStateException();
    }

    protected final void processKeyCommand(int i) {
        keyPressed(i);
        keyReleased(i);
    }
}
