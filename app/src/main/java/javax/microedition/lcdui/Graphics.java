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
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;

import com.mascotcapsule.micro3d.v3.Graphics3D;

import javax.microedition.lcdui.game.Sprite;

public class Graphics implements com.vodafone.v10.graphics.j3d.Graphics3D {
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
	private int canvasInitSave;

	private Paint drawPaint = new Paint();
	private Paint fillPaint = new Paint();

	private int translateX;
	private int translateY;

	private Rect rect = new Rect();
	private Rect clipRect = new Rect();
	private Rect canvasRect = new Rect();
	private RectF rectF = new RectF();
	private Path path = new Path();

	private DashPathEffect dashPathEffect = new DashPathEffect(new float[]{5, 5}, 0);
	private int stroke = SOLID;

	private Font font = Font.getDefaultFont();
	private com.mascotcapsule.micro3d.v3.Graphics3D g3d;

	Graphics(Image image) {
		canvasBitmap = image.getBitmap();
		canvas = new Canvas(canvasBitmap);
		canvas.clipRect(image.getBounds());
		canvasInitSave = canvas.save();
		drawPaint.setStyle(Paint.Style.STROKE);
		fillPaint.setStyle(Paint.Style.FILL);
		drawPaint.setAntiAlias(false);
		fillPaint.setAntiAlias(false);
	}

	public void reset() {
		setColor(0);
		setFont(Font.getDefaultFont());
		setStrokeStyle(SOLID);
		canvas.restoreToCount(canvasInitSave);
		canvasInitSave = canvas.save();
		setClip(0, 0, canvas.getWidth(), canvas.getHeight());
		canvas.getClipBounds(canvasRect);
		clipRect.set(canvasRect);
		translateX = 0;
		translateY = 0;
	}

	public Canvas getCanvas() {
		return canvas;
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
			drawPaint.setPathEffect(dashPathEffect);
		} else {
			drawPaint.setPathEffect(null);
		}
	}

	public int getStrokeStyle() {
		return stroke;
	}

	public void setFont(Font font) {
		if (font == null) {
			font = Font.getDefaultFont();
		}
		this.font = font;
		font.copyInto(fillPaint);
	}

	public Font getFont() {
		return font;
	}

	public void setClip(int x, int y, int width, int height) {
		clipRect.set(x, y, x + width, y + height);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			canvas.restore();
			canvas.save();
			canvas.translate(translateX, translateY);
			canvas.clipRect(clipRect);
		} else {
			canvas.clipRect(clipRect, Region.Op.REPLACE);
		}
		// Calculate the clip
		clipRect.offset(translateX, translateY);
		clipRect.sort();
		if (!clipRect.intersect(canvasRect)) {
			clipRect.set(translateX, translateY, translateX, translateY);
		}
	}

	public void clipRect(int x, int y, int width, int height) {
		canvas.clipRect(x, y, x + width, y + height);
		// Calculate the clip
		clipRect.offset(-translateX, -translateY);
		clipRect.sort();
		clipRect.intersect(x, y, x + width, y + height);
		clipRect.offset(translateX, translateY);
	}

	public int getClipX() {
		return clipRect.left - translateX;
	}

	public int getClipY() {
		return clipRect.top - translateY;
	}

	public int getClipWidth() {
		return clipRect.width();
	}

	public int getClipHeight() {
		return clipRect.height();
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
		if (width < 0 || height < 0) return;
		rectF.set(x, y, x + width, y + height);
		canvas.drawArc(rectF, -startAngle, -arcAngle, false, drawPaint);
	}

	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		if (width <= 0 || height <= 0) return;
		rectF.set(x, y, x + width, y + height);
		canvas.drawArc(rectF, -startAngle, -arcAngle, true, fillPaint);
	}

	public void drawRect(int x, int y, int width, int height) {
		if (width < 0 || height < 0) return;
		canvas.drawRect(x, y, x + width, y + height, drawPaint);
	}

	public void fillRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) return;
		canvas.drawRect(x, y, x + width, y + height, fillPaint);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		if (width < 0 || height < 0) return;
		rectF.set(x, y, x + width, y + height);
		canvas.drawRoundRect(rectF, arcWidth, arcHeight, drawPaint);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		if (width < 0 || height < 0) return;
		rectF.set(x, y, x + width, y + height);
		canvas.drawRoundRect(rectF, arcWidth, arcHeight, fillPaint);
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
		fillPolygon(new int[]{x1, x2, x3}, 0, new int[]{y1, y2, y3}, 0, 3);
	}

	public void drawChar(char character, int x, int y, int anchor) {
		drawChars(new char[]{character}, 0, 1, x, y, anchor);
	}

	public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
		if (anchor == 0) {
			anchor = LEFT | TOP;
		}

		if ((anchor & Graphics.LEFT) != 0) {
			fillPaint.setTextAlign(Paint.Align.LEFT);
		} else if ((anchor & Graphics.RIGHT) != 0) {
			fillPaint.setTextAlign(Paint.Align.RIGHT);
		} else if ((anchor & Graphics.HCENTER) != 0) {
			fillPaint.setTextAlign(Paint.Align.CENTER);
		}

		if ((anchor & Graphics.TOP) != 0) {
			y -= fillPaint.ascent();
		} else if ((anchor & Graphics.BOTTOM) != 0) {
			y -= fillPaint.descent();
		} else if ((anchor & Graphics.VCENTER) != 0) {
			y -= (fillPaint.descent() + fillPaint.ascent()) / 2;
		}

		fillPaint.setAntiAlias(true);
		canvas.drawText(data, offset, length, x, y, fillPaint);
		fillPaint.setAntiAlias(false);
	}

	@SuppressWarnings("unused")
	public void drawString(String text, int x, int y, int anchor) {
		if (anchor == 0) {
			anchor = LEFT | TOP;
		}

		if ((anchor & Graphics.LEFT) != 0) {
			fillPaint.setTextAlign(Paint.Align.LEFT);
		} else if ((anchor & Graphics.RIGHT) != 0) {
			fillPaint.setTextAlign(Paint.Align.RIGHT);
		} else if ((anchor & Graphics.HCENTER) != 0) {
			fillPaint.setTextAlign(Paint.Align.CENTER);
		}

		if ((anchor & Graphics.TOP) != 0) {
			y -= fillPaint.ascent();
		} else if ((anchor & Graphics.BOTTOM) != 0) {
			y -= fillPaint.descent();
		} else if ((anchor & Graphics.VCENTER) != 0) {
			y -= (fillPaint.descent() + fillPaint.ascent()) / 2;
		}

		fillPaint.setAntiAlias(true);
		canvas.drawText(text, x, y, fillPaint);
		fillPaint.setAntiAlias(false);
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

	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
		if (anchor == 0) {
			anchor = LEFT | TOP;
		}

		if ((anchor & Graphics.LEFT) != 0) {
			fillPaint.setTextAlign(Paint.Align.LEFT);
		} else if ((anchor & Graphics.RIGHT) != 0) {
			fillPaint.setTextAlign(Paint.Align.RIGHT);
		} else if ((anchor & Graphics.HCENTER) != 0) {
			fillPaint.setTextAlign(Paint.Align.CENTER);
		}

		if ((anchor & Graphics.TOP) != 0) {
			y -= fillPaint.ascent();
		} else if ((anchor & Graphics.BOTTOM) != 0) {
			y -= fillPaint.descent();
		} else if ((anchor & Graphics.VCENTER) != 0) {
			y -= (fillPaint.descent() + fillPaint.ascent()) / 2;
		}

		fillPaint.setAntiAlias(true);
		canvas.drawText(str, offset, offset + len, x, y, fillPaint);
		fillPaint.setAntiAlias(false);
	}

	public void drawRegion(Image image, int srcx, int srcy, int width, int height,
						   int transform, int dstx, int dsty, int anchor) {
		if (width <= 0 || height <= 0) return;

		if (transform != 0) {
			Rect srcR = new Rect(srcx, srcy, srcx + width, srcy + height);
			RectF dstR = new RectF(0, 0, width, height);
			RectF deviceR = new RectF();
			Matrix matrix = Sprite.transformMatrix(transform, width / 2.0f, height / 2.0f);
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
		if (width <= 0 || height <= 0) return;
		final int[] pixels = new int[width * height];
		canvasBitmap.getPixels(pixels, 0, width, x_src, y_src, width, height);
		if ((anchor & Graphics.RIGHT) != 0) {
			x_dest -= width;
		} else if ((anchor & Graphics.HCENTER) != 0) {
			x_dest -= width / 2;
		}
		if ((anchor & Graphics.BOTTOM) != 0) {
			y_dest -= height;
		} else if ((anchor & Graphics.VCENTER) != 0) {
			y_dest -= height / 2;
		}
		canvas.drawBitmap(pixels, 0, width, x_dest, y_dest, width, height, false, null);
	}

	public void getPixels(int[] pixels, int offset, int stride,
						  int x, int y, int width, int height) {
		Bitmap b = canvasBitmap;
		int w = Math.min(width, b.getWidth() - x);
		int h = Math.min(height, b.getHeight() - y);
		b.getPixels(pixels, offset, stride, x, y, w, h);
	}

	public Bitmap getBitmap() {
		return canvasBitmap;
	}

	void flush(Image image, int x, int y, int width, int height) {
		rect.set(x, y, x + width, y + height);
		canvas.drawBitmap(image.getBitmap(), rect, rect, null);
	}

	@Override
	public synchronized void drawFigure(com.vodafone.v10.graphics.j3d.Figure figure,
										int x, int y,
										com.vodafone.v10.graphics.j3d.FigureLayout layout,
										com.vodafone.v10.graphics.j3d.Effect3D effect) {
		if (g3d == null) g3d = new Graphics3D();
		g3d.bind(this);
		g3d.drawFigure(figure, x, y, layout, effect);
		g3d.release(this);
	}
}
