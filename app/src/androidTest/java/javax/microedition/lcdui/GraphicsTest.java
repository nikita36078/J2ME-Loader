/*
 * Copyright 2018 Nikita Shakarun
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.microedition.lcdui.game.Sprite;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class GraphicsTest {

	private static final int WHITE = 0x00ffffff;
	private static final int BLACK = 0x00000000;
	private static final int RED = 0x00ff0000;
	private static final int GREEN = 0x0000ff00;
	private static final int BLUE = 0x000000ff;
	private static final int RGB_MASK = 0x00FFFFFF;
	private static final int ALPHA_MASK = 0xFF000000;

	private static final int testWidth = 20;
	private static final int testHeight = 20;

	@Before
	public void setUp() throws Exception {
		Font.setApplyDimensions(false);
	}

	@Test
	public void drawLine() {
		Image image = Image.createImage(testWidth, testHeight);
		Graphics graphics = image.getGraphics();

		graphics.setColor(BLUE);
		graphics.setStrokeStyle(Graphics.DOTTED);
		graphics.drawLine(1, 5, 1, 16);

		graphics.setColor(RED);
		graphics.setStrokeStyle(Graphics.SOLID);
		graphics.drawLine(10, 4, 10, 15);

		final int[] spotsToValidate = {
				1, 4, WHITE,
				1, 5, BLUE,
				1, 10, WHITE,
				1, 16, BLUE,
				1, 17, WHITE,

				10, 3, WHITE,
				10, 4, RED,
				10, 15, RED,
				10, 16, WHITE
		};
		assertTrue(validate(image, spotsToValidate));
	}

	@Test
	public void drawImage() {
		Image image = Image.createImage(testWidth, testHeight);
		Graphics graphics = image.getGraphics();

		Image drawImage = Image.createImage(5, 5);
		Graphics drawGraphics = drawImage.getGraphics();
		drawGraphics.fillRect(0, 0, 5, 5);

		graphics.setColor(WHITE);
		graphics.drawImage(drawImage, 0, 0, 0);
		graphics.drawImage(drawImage, 2, 8, 0);

		final int[] spotsToValidate = {
				0, 0, BLACK,
				4, 0, BLACK,
				0, 4, BLACK,
				4, 4, BLACK,

				5, 0, WHITE,
				4, 5, WHITE,

				2, 8, BLACK,
				6, 8, BLACK,
				2, 12, BLACK,
				6, 12, BLACK,

				7, 8, WHITE,
				6, 13, WHITE
		};
		assertTrue(validate(image, spotsToValidate));
	}

	@Test
	public void drawRGB() {
		Image image = Image.createImage(testWidth, testHeight);
		Graphics graphics = image.getGraphics();

		int r = RED | 0x75000000;
		final int[] rgb = {
				r, r, r, r, r,
				r, r, r, r, r,
				r, r, r, r, r,
				r, r, r, r, r,
				r, r, r, r, r
		};
		graphics.setColor(WHITE);
		graphics.drawRGB(rgb, 0, 5, 0, 0, 5, 5, true);
		graphics.drawRGB(rgb, 9, 4, 6, 6, 4, 4, false);

		int PINK = 0xFF8A8A; // Alpha blending check
		final int[] spotsToValidate = {
				0, 0, PINK,
				4, 0, PINK,
				0, 4, PINK,
				4, 4, PINK,

				5, 0, WHITE,
				4, 5, WHITE,

				6, 6, RED,
				9, 6, RED,
				6, 9, RED,
				9, 9, RED,

				10, 6, WHITE,
				9, 10, WHITE
		};
		assertTrue(validate(image, spotsToValidate));

		// check correctness of colors
		int[] in = new int[]{
				0xFF000000, 0xFFFFFFFF, 0xFF888888, 0xFF111111, 0xFFEEEEEE,
				0x88000000, 0x88FFFFFF, 0x88888888, 0x88111111, 0x88EEEEEE,
				0x00000000, 0x00FFFFFF, 0x00888888, 0x00111111, 0x00EEEEEE
		};
		image = Image.createImage(1, in.length);
		graphics = image.getGraphics();
		graphics.drawRGB(in, 0, 1, 0, 0, 1, in.length, false);
		int[] out = new int[in.length];
		image.getRGB(out, 0, 1, 0, 0, 1, in.length);
		for (int i = 0; i < in.length; i++) {
			int e = in[i] |= ALPHA_MASK;
			int a = out[i];
			if (e != a) {
				String msg = String.format("Illegal value at index=%d, expected=%6X, actual=%6X", i, e, a);
				throw new AssertionError(msg);
			}
		}
		image.getBitmap().eraseColor(Color.WHITE);
		graphics.drawRGB(in, 0, 1, 0, 0, 1, in.length, true);
		image.getRGB(out, 0, 1, 0, 0, 1, in.length);
		for (int i = 0; i < in.length; i++) {
			int a = out[i];
			int e = blendPixel(in[i]);
			if (e != a) {
				String msg = String.format("Illegal value at index=%d, expected=%6X, actual=%6X", i, e, a);
				throw new AssertionError(msg);
			}
		}
	}

	@Test
	public void drawRegion() {
		Image image = Image.createImage(testWidth, testHeight);

		Image drawImage = Image.createImage(10, 20);

		drawImage.getGraphics().fillRect(0, 0, 10, 20);
		image.getGraphics().drawRegion(drawImage, 1, 3, 5, 7, Sprite.TRANS_MIRROR_ROT270, 9, 11,
				Graphics.LEFT | Graphics.VCENTER);

		final int[] spotsToValidate = {
				12,  9, BLACK,
				12, 13, BLACK,
				 9, 10, BLACK,
				15, 10, BLACK,

				12,  8, WHITE,
				12, 14, WHITE,
				 8, 10, WHITE,
				16, 10, WHITE
		};
		assertTrue(validate(image, spotsToValidate));
	}

	@Test
	public void setClip() {
		Image image = Image.createImage(testWidth, testHeight);
		Graphics graphics = image.getGraphics();

		graphics.setClip(0, 0, 5, 5);
		graphics.setColor(RED);
		graphics.fillRect(0, 0, testWidth, testHeight);

		graphics.setClip(5, 5, 5, 5);
		graphics.setColor(BLUE);
		graphics.fillRect(0, 0, testWidth, testHeight);

		final int[] spotsToValidate = {
				0, 0, RED,
				4, 0, RED,
				0, 4, RED,
				4, 4, RED,

				5, 0, WHITE,
				4, 5, WHITE,

				5, 5, BLUE,
				9, 5, BLUE,
				5, 9, BLUE,
				9, 9, BLUE,

				10, 5, WHITE,
				9, 10, WHITE
		};
		assertTrue(validate(image, spotsToValidate));
	}

	@Test
	public void clipRect() {
		Image image = Image.createImage(testWidth, testHeight);
		Graphics graphics = image.getGraphics();

		graphics.setClip(0, 0, 10, 10);
		graphics.clipRect(0, 0, 5, 5);
		graphics.setColor(RED);
		graphics.fillRect(0, 0, testWidth, testHeight);

		final int[] spotsToValidate = {
				0, 0, RED,
				4, 0, RED,
				0, 4, RED,
				4, 4, RED,

				5, 0, WHITE,
				4, 5, WHITE,
		};
		assertTrue(validate(image, spotsToValidate));
	}

	private boolean validate(Image image, final int[] spotsToValidate) {
		for (int i = 0; i < spotsToValidate.length; i += 3) {
			int c = getPixel(image, spotsToValidate[i], spotsToValidate[i + 1]);
			if (c != spotsToValidate[i + 2])
				return false;
		}
		return true;
	}

	private int getPixel(Image image, int x, int y) {
		return image.getBitmap().getPixel(x, y) & RGB_MASK;
	}

	public static int blendPixel(int src) {
		float alpha = Color.alpha(src) / 255.0f;
		final float beta = 1 - alpha;
		int r = Math.round(alpha * Color.red(src) + beta * 255.0f);
		int g = Math.round(alpha * Color.green(src) + beta * 255.0f);
		int b = Math.round(alpha * Color.blue(src) + beta * 255.0f);
		return ALPHA_MASK | r << 16 | g << 8 | b;
	}
}