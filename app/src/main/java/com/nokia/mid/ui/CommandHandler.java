package com.nokia.mid.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

final class CommandHandler implements CommandListener {
    private FullCanvas canvas;

    CommandHandler(FullCanvas fullCanvas) {
        this.canvas = fullCanvas;
    }

    public final void commandAction(Command command, Displayable displayable) {
        this.canvas.processKeyCommand(((ButtonCommand) command).key);
    }
}
