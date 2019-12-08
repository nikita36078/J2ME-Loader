package com.siemens.mp.color_game;

import javax.microedition.lcdui.Graphics;

public abstract class Layer {
	int x;

	int y;

	int width;

	int height;

	boolean visible = true;

	Layer(int width, int height) {
		setWidth(width);
		setHeight(height);
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void move(int dx, int dy) {
		x += dx;
		y += dy;
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}

	public final int getWidth() {
		return width;
	}

	public final int getHeight() {
		return height;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public final boolean isVisible() {
		return visible;
	}

	public abstract void paint(Graphics g);

	public void setWidth(int width) {
		if (width < 0) {
			throw new IllegalArgumentException();
		}
		this.width = width;
	}

	public void setHeight(int height) {
		if (height < 0) {
			throw new IllegalArgumentException();
		}
		this.height = height;
	}
}
