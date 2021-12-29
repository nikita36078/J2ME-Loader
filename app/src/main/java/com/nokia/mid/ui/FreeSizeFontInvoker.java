package com.nokia.mid.ui;

import javax.microedition.lcdui.Font;

public class FreeSizeFontInvoker {

    public static void setInvoker(FreeSizeFontInvoker invoker) {
    }

    public static Font getFont(int face, int style, int height) {
        // On Symbian^3, the actual font height is less than given
        return new Font(face, style, -1, (int) ((float)height * 0.78F));
    }
}
