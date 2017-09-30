/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017 Nikita Shakarun
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

import java.lang.reflect.Field;

public class Graphics {
	public static final int HCENTER = 1;
	public static final int VCENTER = 2;
	public static final int LEFT = 4;
	public static final int RIGHT = 8;
	public static final int TOP = 16;
	public static final int BOTTOM = 32;
	public static final int BASELINE = 64;

	public static final int SOLID = 0;
	public static final int DOTTED = 1;

	private Canvas canvas;

	private Paint drawPaint;
	private Paint fillPaint;
	private Paint imagePaint;

	private int translateX;
	private int translateY;

	private Rect intRect;
	private RectF floatRect;
	private Path path;

	private Point windowOrg;
	private Rect windowClip;
	private boolean useWindow;

	private DashPathEffect dpeffect;
	private int stroke;

	private boolean drawAntiAlias;
	private boolean textAntiAlias;

	private Font font;
	private char[] singleChar;

	public Graphics() {
		drawPaint = new Paint();
		fillPaint = new Paint();
		imagePaint = new Paint();

		drawPaint.setStyle(Paint.Style.STROKE);
		fillPaint.setStyle(Paint.Style.FILL);

		imagePaint.setAlpha(255);

		dpeffect = new DashPathEffect(new float[]{5, 5}, 0);
		setStrokeStyle(SOLID);

		setAntiAlias(false);
		setAntiAliasText(true);

		font = Font.getDefaultFont();

		windowOrg = new Point();
		windowClip = new Rect();
		useWindow = false;

		intRect = new Rect();
		floatRect = new RectF();
		path = new Path();
		singleChar = new char[1];
	}

	public Graphics(Canvas canvas) {
		this();
		setCanvas(canvas);
	}

	public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints) {
		if (nPoints > 0) {
			Path path = computePath(xPoints, xOffset, yPoints, yOffset, nPoints);
			canvas.drawPath(path, fillPaint);
		}
	}

	public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints) {
		if (nPoints > 0) {
			Path path = computePath(xPoints, xOffset, yPoints, yOffset, nPoints);
			canvas.drawPath(path, drawPaint);
		}
	}

	private Path computePath(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints) {
		Path path = new Path();
		int i4 = xOffset + 1;
		int i5 = yOffset + 1;
		path.moveTo((float) xPoints[xOffset], (float) yPoints[yOffset]);
		int i6 = i5;
		i5 = i4;
		i4 = 1;
		while (i4 < nPoints) {
			int i7 = i5 + 1;
			int i8 = i6 + 1;
			path.lineTo((float) xPoints[i5], (float) yPoints[i6]);
			i5 = i7;
			i6 = i8;
			i4++;
		}
		path.close();
		return path;
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public boolean hasCanvas() {
		return canvas != null;
	}

	public void setColor(int color) {
		setColorAlpha(color | 0xFF000000);
	}

	public void setColorAlpha(int color) {
		drawPaint.setColor(color);
		fillPaint.setColor(color);
	}

	public void setColor(int r, int g, int b) {
		setColor(255, r, g, b);
	}

	public void setColor(int a, int r, int g, int b) {
		drawPaint.setARGB(a, r, g, b);
		fillPaint.setARGB(a, r, g, b);
	}

	public void setGrayScale(int value) {
		setColor(value, value, value);
	}

	public int getGrayScale() {
		return (getRedComponent() + getGreenComponent() + getBlueComponent()) / 3;
	}

	public int getAlphaComponent() {
		return drawPaint.getAlpha();
	}

	public int getRedComponent() {
		return (drawPaint.getColor() >> 16) & 0xFF;
	}

	public int getGreenComponent() {
		return (drawPaint.getColor() >> 8) & 0xFF;
	}

	public int getBlueComponent() {
		return drawPaint.getColor() & 0xFF;
	}

	public int getColor() {
		return drawPaint.getColor();
	}

	public int getDisplayColor(int color) {
		return color;
	}

	public void setStrokeStyle(int stroke) {
		this.stroke = stroke;

		if (stroke == DOTTED) {
			drawPaint.setPathEffect(dpeffect);
		} else {
			drawPaint.setPathEffect(null);
		}
	}

	public int getStrokeStyle() {
		return stroke;
	}

	public void setAntiAlias(boolean aa) {
		drawAntiAlias = aa;

		drawPaint.setAntiAlias(aa);
		fillPaint.setAntiAlias(aa);
	}

	public void setAntiAliasText(boolean aa) {
		textAntiAlias = aa;
	}

	public void setFont(Font font) {
		this.font = font;
		font.copyInto(drawPaint);
	}

	public Font getFont() {
		return font;
	}

	public void setWindow(int x, int y, int width, int height) {
		windowOrg.set(x, y);
		windowClip.set(0, 0, width, height);

		canvas.translate(x, y);
		canvas.clipRect(windowClip, Region.Op.REPLACE);

		useWindow = true;
	}

	public void resetWindow() {
		canvas.translate(-windowOrg.x, -windowOrg.y);

		windowClip.set(0, 0, canvas.getWidth(), canvas.getHeight());
		canvas.clipRect(windowClip, Region.Op.REPLACE);

		useWindow = false;
	}

	public void resetClip() {
		setClip(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	public void setClip(int x, int y, int width, int height) {
		intRect.set(x, y, x + width, y + height);

		if (useWindow) {
			canvas.clipRect(windowClip, Region.Op.REPLACE);
			canvas.clipRect(intRect, Region.Op.INTERSECT);
		} else {
			canvas.clipRect(intRect, Region.Op.REPLACE);
		}
	}

	public void clipRect(int x, int y, int width, int height) {
		intRect.set(x, y, x + width, y + height);
		canvas.clipRect(intRect, Region.Op.INTERSECT);
	}

	public void subtractClip(int x, int y, int width, int height) {
		intRect.set(x, y, x + width, y + height);
		canvas.clipRect(intRect, Region.Op.DIFFERENCE);
	}

	public int getClipX() {
		return canvas.getClipBounds().left;
	}

	public int getClipY() {
		return canvas.getClipBounds().top;
	}

	public int getClipWidth() {
		return canvas.getClipBounds().width();
	}

	public int getClipHeight() {
		return canvas.getClipBounds().height();
	}

	public void translate(int dx, int dy) {
		translateX += dx;
		translateY += dy;

		canvas.translate(dx, dy);
	}

	public void resetTranslation() {
		translate(-translateX, -translateY);
	}

	public int getTranslateX() {
		return translateX;
	}

	public int getTranslateY() {
		return translateY;
	}

	public void clear(int color) {
		canvas.drawColor(color, PorterDuff.Mode.SRC);
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
		if (x2 >= x1) {
			x2++;
		} else {
			x1++;
		}

		if (y2 >= y1) {
			y2++;
		} else {
			y1++;
		}

		canvas.drawLine(x1, y1, x2, y2, drawPaint);
	}

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		floatRect.set(x, y, x + width, y + height);
		canvas.drawArc(floatRect, -startAngle, -arcAngle, false, drawPaint);
	}

	public void drawArc(RectF oval, int startAngle, int arcAngle) {
		canvas.drawArc(oval, -startAngle, -arcAngle, false, drawPaint);
	}

	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		floatRect.set(x, y, x + width, y + height);
		canvas.drawArc(floatRect, -startAngle, -arcAngle, true, fillPaint);
	}

	public void fillArc(RectF oval, int startAngle, int arcAngle) {
		canvas.drawArc(oval, -startAngle, -arcAngle, true, fillPaint);
	}

	public void drawRect(int x, int y, int width, int height) {
		canvas.drawRect(x, y, x + width, y + height, drawPaint);
	}

	public void drawRect(RectF rect) {
		canvas.drawRect(rect, drawPaint);
	}

	public void fillRect(int x, int y, int width, int height) {
		canvas.drawRect(x, y, x + width, y + height, fillPaint);
	}

	public void fillRect(RectF rect) {
		canvas.drawRect(rect, fillPaint);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		floatRect.set(x, y, x + width, y + height);
		canvas.drawRoundRect(floatRect, arcWidth, arcHeight, drawPaint);
	}

	public void drawRoundRect(RectF rect, int arcWidth, int arcHeight) {
		canvas.drawRoundRect(rect, arcWidth, arcHeight, drawPaint);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		floatRect.set(x, y, x + width, y + height);
		canvas.drawRoundRect(floatRect, arcWidth, arcHeight, fillPaint);
	}

	public void fillRoundRect(RectF rect, int arcWidth, int arcHeight) {
		canvas.drawRoundRect(rect, arcWidth, arcHeight, fillPaint);
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
		path.reset();

		path.moveTo(x1, y1);
		path.lineTo(x2, y2);
		path.lineTo(x3, y3);
		path.close();

		canvas.drawPath(path, fillPaint);
	}

	public void drawChar(char character, int x, int y, int anchor) {
		singleChar[0] = character;
		drawChars(singleChar, 0, 1, x, y, anchor);
	}

	public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
		if (anchor == 0) {
			anchor = LEFT | TOP;
		}

		if ((anchor & Graphics.LEFT) != 0) {
			drawPaint.setTextAlign(Paint.Align.LEFT);
		} else if ((anchor & Graphics.RIGHT) != 0) {
			drawPaint.setTextAlign(Paint.Align.RIGHT);
		} else if ((anchor & Graphics.HCENTER) != 0) {
			drawPaint.setTextAlign(Paint.Align.CENTER);
		}

		if ((anchor & Graphics.TOP) != 0) {
			y -= drawPaint.ascent();
		} else if ((anchor & Graphics.BOTTOM) != 0) {
			y -= drawPaint.descent();
		} else if ((anchor & Graphics.VCENTER) != 0) {
			y -= drawPaint.ascent() + (drawPaint.descent() - drawPaint.ascent()) / 2;
		}

		drawPaint.setAntiAlias(textAntiAlias);
		drawPaint.setStyle(Paint.Style.FILL);
		canvas.drawText(data, offset, length, x, y, drawPaint);
		drawPaint.setStyle(Paint.Style.STROKE);
		drawPaint.setAntiAlias(drawAntiAlias);
	}

	public void drawString(String text, int x, int y, int anchor) {
		if (anchor == 0) {
			anchor = LEFT | TOP;
		}

		if ((anchor & Graphics.LEFT) != 0) {
			drawPaint.setTextAlign(Paint.Align.LEFT);
		} else if ((anchor & Graphics.RIGHT) != 0) {
			drawPaint.setTextAlign(Paint.Align.RIGHT);
		} else if ((anchor & Graphics.HCENTER) != 0) {
			drawPaint.setTextAlign(Paint.Align.CENTER);
		}

		if ((anchor & Graphics.TOP) != 0) {
			y -= drawPaint.ascent();
		} else if ((anchor & Graphics.BOTTOM) != 0) {
			y -= drawPaint.descent();
		} else if ((anchor & Graphics.VCENTER) != 0) {
			y -= drawPaint.ascent() + (drawPaint.descent() - drawPaint.ascent()) / 2;
		}

		drawPaint.setAntiAlias(textAntiAlias);

		drawPaint.setStyle(Paint.Style.FILL);
		canvas.drawText(text, x, y, drawPaint);
		drawPaint.setStyle(Paint.Style.STROKE);
		drawPaint.setAntiAlias(drawAntiAlias);
	}

	public void drawImage(Image image, int x, int y, int anchor) {
		if ((anchor & Graphics.RIGHT) != 0) {
			x -= image.getWidth();
		} else if ((anchor & Graphics.HCENTER) != 0) {
			x -= image.getWidth() / 2;
		}

		if ((anchor & Graphics.BOTTOM) != 0) {
			y -= image.getHeight();
		} else if ((anchor & Graphics.VCENTER) != 0) {
			y -= image.getHeight() / 2;
		}

		canvas.drawBitmap(image.getBitmap(), x, y, null);
	}

	public void drawImage(Image image, int x, int y, int width, int height, boolean filter, int alpha) {
		imagePaint.setFilterBitmap(filter);
		imagePaint.setAlpha(alpha);

		if (width > 0 && height > 0) {
			intRect.set(x, y, x + width, y + height);
			canvas.drawBitmap(image.getBitmap(), null, intRect, imagePaint);
		} else {
			canvas.drawBitmap(image.getBitmap(), x, y, imagePaint);
		}
	}

	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
		int newx = x;
		int newy = y;

		if (anchor == 0) {
			anchor = javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT;
		}

		if ((anchor & javax.microedition.lcdui.Graphics.TOP) != 0) {
			newy -= drawPaint.ascent();
		} else if ((anchor & javax.microedition.lcdui.Graphics.BOTTOM) != 0) {
			newy -= drawPaint.descent();
		}
		if ((anchor & javax.microedition.lcdui.Graphics.HCENTER) != 0) {
			newx -= drawPaint.measureText(str) / 2;
		} else if ((anchor & javax.microedition.lcdui.Graphics.RIGHT) != 0) {
			newx -= drawPaint.measureText(str);
		}

		drawPaint.setStyle(Paint.Style.FILL);
		canvas.drawText(str, offset, len + offset, newx, newy, drawPaint);
		drawPaint.setStyle(Paint.Style.STROKE);
	}

	public void drawRegion(Image image, int srcx, int srcy, int width, int height, int transform, int dstx, int dsty, int anchor) {
		if (width == 0 || height == 0) return;
		drawImage(Image.createImage(image, srcx, srcy, width, height, transform), dstx, dsty, anchor);
	}

	public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
		canvas.drawBitmap(rgbData, offset, scanlength, x, y, width, height, processAlpha, drawPaint);
	}

	public void copyArea(int x_src, int y_src, int width, int height,
						 int x_dest, int y_dest, int anchor) {
		try {
			Field field = canvas.getClass().getDeclaredField("mBitmap");
			field.setAccessible(true);
			Bitmap bitmap = (Bitmap) field.get(canvas);
			Bitmap newBitmap = Bitmap.createBitmap(bitmap, x_src, y_src, width, height);
			drawImage(new Image(newBitmap), x_dest, y_dest, anchor);
		} catch (IllegalAccessException iae) {
			iae.printStackTrace();
		} catch (NoSuchFieldException nsfe) {
			nsfe.printStackTrace();
		}
	}
}
