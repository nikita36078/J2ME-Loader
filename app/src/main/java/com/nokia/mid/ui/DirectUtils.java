package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class DirectUtils {
	public static Image createImage(int i, int i2, int i3) {
		Image createImage = Image.createImage(i, i2);
		Graphics graphics = createImage.getGraphics();
		graphics.setColor(i3);
		graphics.fillRect(0, 0, i, i2);
		graphics.setColor(0);
		return createImage;
	}

	public static Image createImage(byte[] bArr, int i, int i2) {
		Image createImage = Image.createImage(bArr, i, i2);
		Image createImage2 = Image.createImage(createImage.getWidth(), createImage.getHeight());
		createImage2.getGraphics().drawImage(createImage, 0, 0, 0);
		return createImage2;
	}

	public static DirectGraphics getDirectGraphics(Graphics graphics) {
		return new DirectGraphicsImp(graphics);
	}
}
