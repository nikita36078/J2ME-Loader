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

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.util.Arrays;

import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.config.ProfileModel;

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

	public static final int FONT_STATIC_TEXT = 0;
	public static final int FONT_INPUT_TEXT = 1;

	private static final int[] SCREEN_SIZES = {128, 176, 220, 320};
	private static final int[] FONT_SIZES = {
			9, 13, 15, // 128
			13, 15, 20, // 176
			15, 18, 22, // 220
			18, 22, 26, // 320
	};

	private static final int FONT_COUNT = 3 * 3 * 2 * 2 * 2;
	private static final Font[] fonts = new Font[FONT_COUNT];
	private static final float[] sizes = {22, 18, 26};

	private static boolean antiAlias;

	final Paint paint = new Paint();
	final float ascent;
	final float descent;
	private final int height;
	private final int face;
	private final int style;
	private final int size;

	@SuppressLint("WrongConstant")
	public Font(int face, int style, int size, float height) {
		this.face = face;
		this.style = style;
		this.size = size;

		Typeface family;
		switch (face) {
			case FACE_MONOSPACE:
				family = Typeface.MONOSPACE;
				break;
			case FACE_PROPORTIONAL:
				family = Typeface.SANS_SERIF;
				break;
			default:
				family = Typeface.DEFAULT;
		}

		paint.setColor(Color.BLACK);
		paint.setTypeface(Typeface.create(family, style & Typeface.BOLD_ITALIC));
		paint.setAntiAlias(antiAlias);
		paint.setStyle(Paint.Style.FILL);
		paint.setUnderlineText((style & STYLE_UNDERLINED) != 0);

		// at first, just set the size (no matter what is put here)
		paint.setTextSize(height);
		// and now we set the size equal to the given one (in pixels)
		paint.setTextSize(height * height / paint.getFontSpacing());

		Paint.FontMetrics fm = new Paint.FontMetrics();
		this.height = (int) Math.ceil(paint.getFontMetrics(fm));
		this.ascent = fm.ascent;
		this.descent = fm.descent;
	}

	public static Font getFont(int fontSpecifier) {
		return getDefaultFont();
	}

	public static Font getFont(int face, int style, int size) {
		int index = ((face >> 5) * 3 + (size >> 3) << 3) + style;
		Font font = fonts[index];

		if (font == null) {
			float height = sizes[size / 8];
			font = new Font(face, style, size, height);
			fonts[index] = font;
		}

		return font;
	}

	public static Font getDefaultFont() {
		return getFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
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

	public int substringWidth(String str, int offset, int len) {
		return (int) paint.measureText(str, offset, offset + len);
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

	public static void applySettings(ProfileModel params) {
		antiAlias = params.fontAA;

		float small = params.fontSizeSmall;
		float medium = params.fontSizeMedium;
		float large = params.fontSizeLarge;

		int screen = Math.max(params.screenWidth, params.screenHeight);
		if (medium <= 0) medium = Font.getFontSizeForResolution(1, screen);
		if (small <= 0) small = Font.getFontSizeForResolution(0, screen);
		if (large <= 0) large = Font.getFontSizeForResolution(2, screen);

		if (params.fontApplyDimensions) {
			DisplayMetrics metrics = ContextHolder.getAppContext().getResources().getDisplayMetrics();
			medium = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, medium, metrics);
			small = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, small, metrics);
			large = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, large, metrics);
		}
		sizes[0] = medium;
		sizes[1] = small;
		sizes[2] = large;

		Arrays.fill(fonts, null);
	}

	private static int getFontSizeForResolution(int type, int size) {
		if (size > 0) {
			for (int i = 0; i < SCREEN_SIZES.length; i++) {
				if (SCREEN_SIZES[i] >= size) {
					return FONT_SIZES[i * 3 + type];
				}
			}
		}
		return FONT_SIZES[FONT_SIZES.length - 3 + type];
	}
}
