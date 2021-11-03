package com.kddi.system;

import javax.microedition.lcdui.keyboard.VirtualKeyboard;
import javax.microedition.util.ContextHolder;

public class PhoneSystem {
    public PhoneSystem(){};

    public static int getKeyState(boolean eightDirections) {
        VirtualKeyboard vk = ContextHolder.getVk();
        return vk.getKeyStatesVodafone();
    }

}
