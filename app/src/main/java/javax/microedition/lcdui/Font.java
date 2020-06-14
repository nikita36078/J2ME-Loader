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

	private static final int FONT_COUNT = 3 * 3 * (1 << 3);
	private static Font[] fonts = new Font[FONT_COUNT];

	private static boolean applyDimensions = true;
	private static float[] sizes = new float[]{18, 22, 26};

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

	private Paint paint;
	private int face, style, size;

	public Font(Typeface face, int style, float size, boolean underline) {
		if (applyDimensions) {
			DisplayMetrics metrics = ContextHolder.getAppContext().getResources().getDisplayMetrics();
			size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, metrics);
		}

		paint = new Paint();

		paint.setTypeface(Typeface.create(face, style));
		paint.setUnderlineText(underline);

		paint.setTextSize(size);                                             // at first, just set the size (no matter what is put here)
		paint.setTextSize(size * size / (paint.descent() - paint.ascent())); // and now we set the size equal to the given one (in pixels)
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

	public void copyInto(Paint target) {
		target.setTypeface(paint.getTypeface());
		target.setUnderlineText(paint.isUnderlineText());
		target.setTextSize(paint.getTextSize());
	}

	public Typeface getTypeface() {
		return paint.getTypeface();
	}

	public float getTextSize() {
		return paint.getTextSize();
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
		return (int) Math.ceil(paint.descent() - paint.ascent());
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

	public static int getFontIndex(int face, int style, int size) {
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

	public static int getFontFace(int index) {
		index = (index >>> 3) / 3;

		switch (index) {
			case 0:
				return FACE_MONOSPACE;

			case 1:
				return FACE_PROPORTIONAL;

			case 2:
			default:
				return FACE_SYSTEM;
		}
	}

	public static int getFontSize(int index) {
		index = (index >>> 3) % 3;

		switch (index) {
			case 0:
				return SIZE_SMALL;

			case 1:
			default:
				return SIZE_MEDIUM;

			case 2:
				return SIZE_LARGE;
		}
	}

	public static int getFontStyle(int index) {
		return index & 7;
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
