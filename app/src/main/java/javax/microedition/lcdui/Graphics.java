/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;

import javax.microedition.lcdui.game.Sprite;

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
	private Bitmap canvasBitmap;

	private Paint drawPaint = new Paint();
	private Paint fillPaint = new Paint();
	private Paint imagePaint = new Paint();

	private int translateX;
	private int translateY;

	private Rect intRect = new Rect();
	private RectF floatRect = new RectF();
	private Path path = new Path();

	private DashPathEffect dpeffect = new DashPathEffect(new float[]{5, 5}, 0);
	private int stroke;

	private boolean drawAntiAlias;
	private boolean textAntiAlias;

	private Font font = Font.getDefaultFont();

	public Graphics() {
		drawPaint.setStyle(Paint.Style.STROKE);
		fillPaint.setStyle(Paint.Style.FILL);
		setStrokeStyle(SOLID);
		setAntiAlias(false);
		setAntiAliasText(true);
	}

	public void reset() {
		setColor(0);
		setFont(Font.getDefaultFont());
		setStrokeStyle(SOLID);
		resetClip();
		resetTranslation();
	}

	private void resetTranslation() {
		translateX = 0;
		translateY = 0;
	}

	private void resetClip() {
		setClip(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	public void setCanvas(Canvas canvas, Bitmap canvasBitmap) {
		if (canvas.getSaveCount() > 1) {
			canvas.restoreToCount(1);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			canvas.save();
		}
		canvas.save();
		this.canvas = canvas;
		this.canvasBitmap = canvasBitmap;
	}

	public void setSurfaceCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public boolean hasCanvas() {
		return canvas != null;
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
		path.reset();
		path.moveTo((float) xPoints[xOffset], (float) yPoints[yOffset]);
		for (int i = 1; i < nPoints; i++) {
			path.lineTo((float) xPoints[xOffset + i], (float) yPoints[yOffset + i]);
		}
		path.close();
		return path;
	}

	public void setColor(int color) {
		setColorAlpha(color | 0xFF000000);
	}

	public void setColorAlpha(int color) {
		drawPaint.setColor(color);
		fillPaint.setColor(color);
	}

	public void setColor(int r, int g, int b) {
		drawPaint.setARGB(255, r, g, b);
		fillPaint.setARGB(255, r, g, b);
	}

	public void setGrayScale(int value) {
		setColor(value, value, value);
	}

	public int getGrayScale() {
		return (getRedComponent() + getGreenComponent() + getBlueComponent()) / 3;
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

	private void setAntiAlias(boolean aa) {
		drawAntiAlias = aa;

		drawPaint.setAntiAlias(aa);
		fillPaint.setAntiAlias(aa);
	}

	private void setAntiAliasText(boolean aa) {
		textAntiAlias = aa;
	}

	public void setFont(Font font) {
		if (font == null) {
			font = Font.getDefaultFont();
		}
		this.font = font;
		font.copyInto(drawPaint);
	}

	public Font getFont() {
		return font;
	}

	public void setClip(int x, int y, int width, int height) {
		intRect.set(x, y, x + width, y + height);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			canvas.restore();
			canvas.save();
			canvas.translate(translateX, translateY);
			canvas.clipRect(intRect);
		} else {
			canvas.clipRect(intRect, Region.Op.REPLACE);
		}
	}

	public void clipRect(int x, int y, int width, int height) {
		intRect.set(x, y, x + width, y + height);
		canvas.clipRect(intRect);
	}

	public int getClipX() {
		canvas.getClipBounds(intRect);
		return intRect.left;
	}

	public int getClipY() {
		canvas.getClipBounds(intRect);
		return intRect.top;
	}

	public int getClipWidth() {
		canvas.getClipBounds(intRect);
		return intRect.width();
	}

	public int getClipHeight() {
		canvas.getClipBounds(intRect);
		return intRect.height();
	}

	public void translate(int dx, int dy) {
		translateX += dx;
		translateY += dy;

		canvas.translate(dx, dy);
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

	public void fillRect(int x, int y, int width, int height) {
		canvas.drawRect(x, y, x + width, y + height, fillPaint);
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
		fillPolygon(new int[]{x1, x2, x3}, 0, new int[]{y1, y2, y3}, 0, 3);
	}

	public void drawChar(char character, int x, int y, int anchor) {
		drawChars(new char[]{character}, 0, 1, x, y, anchor);
	}

	public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
		drawString(new String(data, offset, length), x, y, anchor);
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
		drawString(str.substring(offset, len + offset), x, y, anchor);
	}

	public void drawRegion(Image image, int srcx, int srcy, int width, int height, int transform, int dstx, int dsty, int anchor) {
		if (width == 0 || height == 0) return;

		if (transform != 0) {
			Rect srcR = new Rect(srcx, srcy, srcx + width, srcy + height);
			RectF dstR = new RectF(0, 0, width, height);
			RectF deviceR = new RectF();
			Matrix matrix = Sprite.transformMatrix(transform, width / 2, height / 2);
			matrix.mapRect(deviceR, dstR);

			if ((anchor & Graphics.RIGHT) != 0) {
				dstx -= deviceR.width();
			} else if ((anchor & Graphics.HCENTER) != 0) {
				dstx -= deviceR.width() / 2;
			}
			if ((anchor & Graphics.BOTTOM) != 0) {
				dsty -= deviceR.height();
			} else if ((anchor & Graphics.VCENTER) != 0) {
				dsty -= deviceR.height() / 2;
			}

			canvas.save();
			canvas.translate(-deviceR.left + dstx, -deviceR.top + dsty);
			canvas.concat(matrix);
			canvas.drawBitmap(image.getBitmap(), srcR, dstR, null);
			canvas.restore();
		} else {
			if ((anchor & Graphics.RIGHT) != 0) {
				dstx -= width;
			} else if ((anchor & Graphics.HCENTER) != 0) {
				dstx -= width / 2;
			}
			if ((anchor & Graphics.BOTTOM) != 0) {
				dsty -= height;
			} else if ((anchor & Graphics.VCENTER) != 0) {
				dsty -= height / 2;
			}

			Rect srcR = new Rect(srcx, srcy, srcx + width, srcy + height);
			RectF dstR = new RectF(dstx, dsty, dstx + width, dsty + height);
			canvas.drawBitmap(image.getBitmap(), srcR, dstR, null);
		}
	}

	public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
		if (width == 0 || height == 0) return;
		// MIDP allows almost any value of scanlength, drawBitmap is more strict with the stride
		if (scanlength < width) {
			scanlength = width;
		}
		int rows = rgbData.length / scanlength;
		if (rows < height) {
			height = rows;
		}
		if (!processAlpha) {
			for (int iy = 0; iy < height; iy++) {
				for (int ix = 0; ix < width; ix++) {
					rgbData[offset + ix + iy * scanlength] |= (0xFF << 24);
				}
			}
		}
		// Use deprecated method due to perfomance issues
		canvas.drawBitmap(rgbData, offset, scanlength, x, y, width, height, processAlpha, null);
	}

	public void copyArea(int x_src, int y_src, int width, int height,
						 int x_dest, int y_dest, int anchor) {
		Bitmap bitmap = Bitmap.createBitmap(canvasBitmap, x_src, y_src, width, height);
		drawImage(new Image(bitmap), x_dest, y_dest, anchor);
	}

	public void getPixels(int[] pixels, int offset, int stride,
						  int x, int y, int width, int height) {
		canvasBitmap.getPixels(pixels, offset, stride, x, y, width, height);
	}

	public Bitmap getBitmap() {
		return canvasBitmap;
	}
}
