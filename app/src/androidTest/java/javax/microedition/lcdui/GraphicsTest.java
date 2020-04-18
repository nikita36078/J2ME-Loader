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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class GraphicsTest {

	private final int WHITE = 0x00ffffff;
	private final int BLACK = 0x00000000;
	private final int RED = 0x00ff0000;
	private final int GREEN = 0x0000ff00;
	private final int BLUE = 0x000000ff;
	private final int RGB_MASK = 0x00ffffff;

	private final int testWidth = 20;
	private final int testHeight = 20;

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
	}

	@Test
	public void drawRegion() {
		Image image = Image.createImage(testWidth, testHeight);
		Graphics graphics = image.getGraphics();

		Image drawImage = Image.createImage(5, 5);
		Graphics drawGraphics = drawImage.getGraphics();
		drawGraphics.fillRect(0, 0, 5, 5);

		graphics.drawRegion(drawImage, 0, 0, 2, 2, 0, 5, 5, 0);

		final int[] spotsToValidate = {
				5, 5, BLACK,
				6, 5, BLACK,
				5, 6, BLACK,
				6, 6, BLACK,

				4, 5, WHITE,
				7, 5, WHITE,
				7, 6, WHITE,
				4, 6, WHITE
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
}