package com.nokia.mid.ui;

import javax.microedition.lcdui.Font;

public class FreeSizeFontInvoker {

    public static void setInvoker(FreeSizeFontInvoker invoker) {
    }

    public static Font getFont(int aFace, int aStyle, int aHeight) {
        // On Symbian^3, the actual font height is less than given
        return new Font(aFace, aStyle, -1, (int) ((float)aHeight * 0.78F));
    }
}
