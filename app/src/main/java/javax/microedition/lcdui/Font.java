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

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.util.Arrays;

import javax.microedition.util.ContextHolder;

public class Font {
	public static final int FACE_MONOSPACE = 32;
	public static final int FACE_PROPORTIONAL = 64;
	public static final int FACE_SYSTEM = 0;

	public static final int SIZE_LARGE = 16;
	public static final int SIZE_MEDIUM = 0;
	public static final int SIZE_SMALL = 8;

	public static final int STYLE_BOLD = 1;
	public static final int STYLE_ITALIC = 2;
	public static final int STYLE_PLAIN = 0;
	public static final int STYLE_UNDERLINED = 4;

	private static final int[] SCREEN_SIZES = {128, 176, 220, 320};
	private static final int[] FONT_SIZES = {
			9, 13, 15, // 128
			13, 15, 20, // 176
			15, 18, 22, // 220
			18, 22, 26, // 320
	};

	private static final int FONT_COUNT = 3 * 3 * (1 << 3);
	private static final Font[] fonts = new Font[FONT_COUNT];
	private static final float[] sizes = new float[]{18, 22, 26};

	private static boolean applyDimensions = true;
	private static boolean antiAlias;

	final Paint paint;
	final float ascent;
	final float descent;
	private final int height;
	private int face, style, size;

	public static void setApplyDimensions(boolean flag) {
		applyDimensions = flag;
		Arrays.fill(fonts, null);
	}

	public static void setSize(int size, float value) {
		switch (size) {
			case SIZE_SMALL:
				sizes[0] = value;
				break;

			case SIZE_MEDIUM:
				sizes[1] = value;
				break;

			case SIZE_LARGE:
				sizes[2] = value;
				break;

			default:
				return;
		}

		Arrays.fill(fonts, null);
	}

	public Font(Typeface face, int style, float size, boolean underline) {
		if (applyDimensions) {
			DisplayMetrics metrics = ContextHolder.getAppContext().getResources().getDisplayMetrics();
			size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, metrics);
		}

		paint = new Paint();
		paint.setAntiAlias(antiAlias);
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.FILL);

		paint.setTypeface(Typeface.create(face, style));
		paint.setUnderlineText(underline);

		// at first, just set the size (no matter what is put here)
		paint.setTextSize(size);
		// and now we set the size equal to the given one (in pixels)
		paint.setTextSize(size * size / paint.getFontSpacing());
		Paint.FontMetrics fm = paint.getFontMetrics();
		height = (int) Math.ceil(fm.leading + fm.bottom - fm.top);
		ascent = fm.ascent;
		descent = fm.descent;
	}

	public static Font getFont(int fontSpecifier) {
		return getDefaultFont();
	}

	public static Font getFont(int face, int style, int size) {
		int index = getFontIndex(face, style, size);

		if (fonts[index] == null) {
			Typeface typeface;
			int tfstyle = Typeface.NORMAL;
			boolean underline;
			float fsize;

			switch (face) {
				case FACE_MONOSPACE:
					typeface = Typeface.MONOSPACE;
					break;

				case FACE_PROPORTIONAL:
					typeface = Typeface.SANS_SERIF;
					break;

				default:
				case FACE_SYSTEM:
					typeface = Typeface.DEFAULT;
					break;
			}

			if ((style & STYLE_BOLD) != 0) {
				tfstyle |= Typeface.BOLD;
			}

			if ((style & STYLE_ITALIC) != 0) {
				tfstyle |= Typeface.ITALIC;
			}

			underline = (style & STYLE_UNDERLINED) != 0;

			switch (size) {
				case SIZE_SMALL:
					fsize = sizes[0];
					break;

				default:
				case SIZE_MEDIUM:
					fsize = sizes[1];
					break;

				case SIZE_LARGE:
					fsize = sizes[2];
					break;
			}

			fonts[index] = new Font(typeface, tfstyle, fsize, underline);

			fonts[index].face = face;
			fonts[index].style = style;
			fonts[index].size = size;
		}

		return fonts[index];
	}

	public static Font getDefaultFont() {
		return getFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
	}

	public static void setAntiAlias(boolean enable) {
		antiAlias = enable;
		Arrays.fill(fonts, null);
	}

	// non api
	public static int getFontSizeForResolution(int sizeType, int width, int height) {
		int size = Math.max(width, height);
		if (size > 0) {
			for (int i = 0; i < SCREEN_SIZES.length; i++) {
				if (SCREEN_SIZES[i] >= size) {
					return FONT_SIZES[i * 3 + sizeType];
				}
			}
		}
		return FONT_SIZES[FONT_SIZES.length - 3 + sizeType];
	}

	public int getFace() {
		return face;
	}

	public int getStyle() {
		return style;
	}

	public int getSize() {
		return size;
	}

	public boolean isUnderlined() {
		return paint.isUnderlineText();
	}

	public int getHeight() {
		return height;
	}

	public int getBaselinePosition() {
		return (int) Math.ceil(-paint.ascent());
	}

	public int charWidth(char c) {
		return (int) Math.ceil(paint.measureText(new char[]{c}, 0, 1));
	}

	public int charsWidth(char[] ch, int offset, int length) {
		return (int) Math.ceil(paint.measureText(ch, offset, length));
	}

	public int stringWidth(String text) {
		return (int) Math.ceil(paint.measureText(text));
	}

	public int substringWidth(String str, int i, int i2) {
		return (int) paint.measureText(str, i, i + i2);
	}

	private static int getFontIndex(int face, int style, int size) {
		switch (face) {
			case FACE_MONOSPACE:
				face = 0;
				break;

			case FACE_PROPORTIONAL:
				face = 1;
				break;

			case FACE_SYSTEM:
				face = 2;
				break;
		}

		switch (size) {
			case SIZE_SMALL:
				size = 0;
				break;

			case SIZE_MEDIUM:
				size = 1;
				break;

			case SIZE_LARGE:
				size = 2;
				break;
		}

		return ((face * 3 + size) << 3) + style;
	}

	public boolean isBold() {
		return style == STYLE_BOLD;
	}

	public boolean isPlain() {
		return style == STYLE_PLAIN;
	}

	public boolean isItalic() {
		return style == STYLE_ITALIC;
	}
}
