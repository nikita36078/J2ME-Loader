package javax.microedition.lcdui.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;

import javax.microedition.lcdui.Image;
import javax.microedition.util.ContextHolder;

public class CanvasWrapper {
	private static final float TEXT_SIZE_KEYBOARD = 22;

	private final float textAscent;
	private final float textCenterOffset;
	private final Paint drawPaint = new Paint();
	private final Paint fillPaint = new Paint();
	private final Paint textPaint = new Paint();
	private final Paint imgPaint = new Paint();
	private Canvas canvas;
	private float textHeight;

	public CanvasWrapper(boolean filterBitmap) {
		imgPaint.setFilterBitmap(filterBitmap);
		drawPaint.setStyle(Paint.Style.STROKE);
		fillPaint.setStyle(Paint.Style.FILL);

		// init text paint
		Resources res = ContextHolder.getAppContext().getResources();
		Typeface typeface = Typeface.createFromAsset(res.getAssets(), "Roboto-Regular.ttf");
		textPaint.setTypeface(typeface);
		float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_KEYBOARD, res.getDisplayMetrics());
		textPaint.setTextSize(size);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textAscent = textPaint.ascent();
		float descent = textPaint.descent();
		textHeight = descent - textAscent;
		textCenterOffset = ((descent + textAscent) / 2);
	}

	public void bind(Canvas canvas) {
		this.canvas = canvas;
	}

	public void clear(int color) {
		canvas.drawColor(color, PorterDuff.Mode.SRC);
	}

	public void drawArc(RectF oval, int startAngle, int sweepAngle) {
		canvas.drawArc(oval, startAngle, sweepAngle, false, drawPaint);
	}

	public void fillArc(RectF oval, int startAngle, int sweepAngle) {
		canvas.drawArc(oval, startAngle, sweepAngle, false, fillPaint);
	}

	public void drawRoundRect(RectF rect, int rx, int ry) {
		canvas.drawRoundRect(rect, rx, ry, drawPaint);
	}

	public void fillRoundRect(RectF rect, int rx, int ry) {
		canvas.drawRoundRect(rect, rx, ry, fillPaint);
	}

	public void drawString(String text, float x, float y) {
		canvas.drawText(text, x, y - textCenterOffset, textPaint);
	}

	public void drawImage(Image image, RectF dst) {
		Bitmap bitmap = image.getBitmap();
		bitmap.prepareToDraw();
		canvas.drawBitmap(bitmap, image.getBounds(), dst, imgPaint);
	}

	public void fillRect(RectF rect) {
		canvas.drawRect(rect, fillPaint);
	}

	public void drawRect(RectF rect) {
		canvas.drawRect(rect, drawPaint);
	}

	public void setDrawColor(int color) {
		drawPaint.setColor(color);
	}

	public void setFillColor(int color) {
		fillPaint.setColor(color);
	}

	public void setTextColor(int color) {
		textPaint.setColor(color);
	}

	public void drawBackgroundedText(String text) {
		float width = textPaint.measureText(text);
		canvas.drawRect(0, 0, width, textHeight, fillPaint);
		canvas.drawText(text, width / 2.0f, -textAscent, textPaint);
	}
}
