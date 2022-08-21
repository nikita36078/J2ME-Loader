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

package com.siemens.mp.color_game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite extends Layer {

	/**
	 * If this bit is set, it denotes that the transform causes the
	 * axes to be interchanged
	 */
	private static final int INVERTED_AXES = 0x4;

	/**
	 * If this bit is set, it denotes that the transform causes the
	 * x axis to be flipped.
	 */
	private static final int X_FLIP = 0x2;

	/**
	 * If this bit is set, it denotes that the transform causes the
	 * y axis to be flipped.
	 */
	private static final int Y_FLIP = 0x1;

	/**
	 * Bit mask for channel value in ARGB pixel.
	 */
	private static final int ALPHA_BITMASK = 0xff000000;

	/**
	 * Alpha channel value for full opacity.
	 */
	private static final int FULLY_OPAQUE_ALPHA = 0xff000000;

	Image sourceImage;

	int numberFrames;

	int[] frameCoordsX;

	int[] frameCoordsY;

	int srcFrameWidth;

	int srcFrameHeight;

	int[] frameSequence;

	private int sequenceIndex;

	private boolean customSequenceDefined;

	int collisionRectX;

	int collisionRectY;

	int collisionRectWidth;

	int collisionRectHeight;

	public Sprite(Image image) {
		super(image.getWidth(), image.getHeight());

		initializeFrames(image, image.getWidth(), image.getHeight(), false);

		// initialize collision rectangle
		initCollisionRectBounds();

	}

	public Sprite(Image image, int frameWidth, int frameHeight) {

		super(frameWidth, frameHeight);
		// if img is null img.getWidth() will throw NullPointerException
		if ((frameWidth < 1 || frameHeight < 1) ||
				((image.getWidth() % frameWidth) != 0) ||
				((image.getHeight() % frameHeight) != 0)) {
			throw new IllegalArgumentException();
		}

		// construct the array of images that
		// we use as "frames" for the sprite.
		// use default frame , sequence index = 0
		initializeFrames(image, frameWidth, frameHeight, false);

		// initialize collision rectangle
		initCollisionRectBounds();

	}

	public Sprite(Sprite s) {

		super(s != null ? s.getWidth() : 0,
				s != null ? s.getHeight() : 0);

		if (s == null) {
			throw new NullPointerException();
		}

		this.sourceImage = Image.createImage(s.sourceImage);

		this.numberFrames = s.numberFrames;

		this.frameCoordsX = new int[this.numberFrames];
		this.frameCoordsY = new int[this.numberFrames];

		System.arraycopy(s.frameCoordsX, 0,
				this.frameCoordsX, 0,
				s.getRawFrameCount());

		System.arraycopy(s.frameCoordsY, 0,
				this.frameCoordsY, 0,
				s.getRawFrameCount());

		this.x = s.getX();
		this.y = s.getY();

		// these fields are set when defining a collision rectangle
		this.collisionRectX = s.collisionRectX;
		this.collisionRectY = s.collisionRectY;
		this.collisionRectWidth = s.collisionRectWidth;
		this.collisionRectHeight = s.collisionRectHeight;

		// these fields are set when creating a Sprite from an Image
		this.srcFrameWidth = s.srcFrameWidth;
		this.srcFrameHeight = s.srcFrameHeight;

		this.setVisible(s.isVisible());

		this.frameSequence = new int[s.getFrameSequenceLength()];
		this.setFrameSequence(s.frameSequence);
		this.setFrame(s.getFrame());
	}

	public void setFrame(int inp_sequenceIndex) {
		if (inp_sequenceIndex < 0 ||
				inp_sequenceIndex >= frameSequence.length) {
			throw new IndexOutOfBoundsException();
		}
		sequenceIndex = inp_sequenceIndex;
	}

	public final int getFrame() {
		return sequenceIndex;
	}

	public int getRawFrameCount() {
		return numberFrames;
	}

	public int getFrameSequenceLength() {
		return frameSequence.length;
	}

	public void nextFrame() {
		sequenceIndex = (sequenceIndex + 1) % frameSequence.length;
	}

	public void prevFrame() {
		if (sequenceIndex == 0) {
			sequenceIndex = frameSequence.length - 1;
		} else {
			sequenceIndex--;
		}
	}

	@Override
	public final void paint(Graphics g) {
		// managing the painting order is the responsibility of
		// the layermanager, so depth is ignored
		if (g == null) {
			throw new NullPointerException();
		}

		if (visible) {

			// width and height of the source
			// image is the width and height
			// of the original frame
			g.drawRegion(sourceImage,
					frameCoordsX[frameSequence[sequenceIndex]],
					frameCoordsY[frameSequence[sequenceIndex]],
					srcFrameWidth,
					srcFrameHeight,
					0,
					this.x,
					this.y,
					Graphics.TOP | Graphics.LEFT);
		}

	}

	public void setFrameSequence(int[] sequence) {

		if (sequence == null) {
			// revert to the default sequence
			sequenceIndex = 0;
			customSequenceDefined = false;
			frameSequence = new int[numberFrames];
			// copy frames indices into frameSequence
			for (int i = 0; i < numberFrames; i++) {
				frameSequence[i] = i;
			}
			return;
		}

		if (sequence.length < 1) {
			throw new IllegalArgumentException();
		}

		for (int aSequence : sequence) {
			if (aSequence < 0 || aSequence >= numberFrames) {
				throw new ArrayIndexOutOfBoundsException();
			}
		}
		customSequenceDefined = true;
		frameSequence = new int[sequence.length];
		System.arraycopy(sequence, 0, frameSequence, 0, sequence.length);
		sequenceIndex = 0;
	}

	public void setImage(Image img, int frameWidth, int frameHeight) {

		// if image is null image.getWidth() will throw NullPointerException
		if ((frameWidth < 1 || frameHeight < 1) ||
				((img.getWidth() % frameWidth) != 0) ||
				((img.getHeight() % frameHeight) != 0)) {
			throw new IllegalArgumentException();
		}

		int noOfFrames =
				(img.getWidth() / frameWidth) * (img.getHeight() / frameHeight);

		boolean maintainCurFrame = true;
		if (noOfFrames < numberFrames) {
			// use default frame , sequence index = 0
			maintainCurFrame = false;
			customSequenceDefined = false;
		}

		if (!((srcFrameWidth == frameWidth) &&
				(srcFrameHeight == frameHeight))) {
			setWidth(frameWidth);
			setHeight(frameHeight);

			initializeFrames(img, frameWidth, frameHeight, maintainCurFrame);

			// initialize collision rectangle
			initCollisionRectBounds();

		} else {
			// just reinitialize the animation frames.
			initializeFrames(img, frameWidth, frameHeight, maintainCurFrame);
		}

	}

	public final boolean collidesWith(Sprite s, boolean pixelLevel) {

		// check if either of the Sprite's are not visible
		if (!(s.visible && this.visible)) {
			return false;
		}

		// these are package private
		// and can be accessed directly
		int otherLeft = s.x + s.collisionRectX;
		int otherTop = s.y + s.collisionRectY;
		int otherRight = otherLeft + s.collisionRectWidth;
		int otherBottom = otherTop + s.collisionRectHeight;

		int left = this.x + this.collisionRectX;
		int top = this.y + this.collisionRectY;
		int right = left + this.collisionRectWidth;
		int bottom = top + this.collisionRectHeight;

		// check if the collision rectangles of the two sprites intersect
		if (intersectRect(otherLeft, otherTop, otherRight, otherBottom,
				left, top, right, bottom)) {

			// collision rectangles intersect
			if (pixelLevel) {

				// we need to check pixel level collision detection.
				// use only the coordinates within the Sprite frame if
				// the collision rectangle is larger than the Sprite
				// frame
				if (this.collisionRectX < 0) {
					left = this.x;
				}
				if (this.collisionRectY < 0) {
					top = this.y;
				}
				if ((this.collisionRectX + this.collisionRectWidth)
						> this.width) {
					right = this.x + this.width;
				}
				if ((this.collisionRectY + this.collisionRectHeight)
						> this.height) {
					bottom = this.y + this.height;
				}

				// similarly for the other Sprite
				if (s.collisionRectX < 0) {
					otherLeft = s.x;
				}
				if (s.collisionRectY < 0) {
					otherTop = s.y;
				}
				if ((s.collisionRectX + s.collisionRectWidth)
						> s.width) {
					otherRight = s.x + s.width;
				}
				if ((s.collisionRectY + s.collisionRectHeight)
						> s.height) {
					otherBottom = s.y + s.height;
				}

				// recheck if the updated collision area rectangles intersect
				if (!intersectRect(otherLeft, otherTop, otherRight, otherBottom,
						left, top, right, bottom)) {

					// if they don't intersect, return false;
					return false;
				}

				// the updated collision rectangles intersect,
				// go ahead with collision detection


				// find intersecting region,
				// within the collision rectangles
				int intersectLeft = (left < otherLeft) ? otherLeft : left;
				int intersectTop = (top < otherTop) ? otherTop : top;

				// used once, optimize.
				int intersectRight = (right < otherRight)
						? right : otherRight;
				int intersectBottom = (bottom < otherBottom)
						? bottom : otherBottom;

				int intersectWidth = Math.abs(intersectRight - intersectLeft);
				int intersectHeight = Math.abs(intersectBottom - intersectTop);

				// have the coordinates in painter space,
				// need coordinates of top left and width, height
				// in source image of Sprite.

				int thisImageXOffset = getImageTopLeftX(intersectLeft);

				int thisImageYOffset = getImageTopLeftY(intersectTop);

				int otherImageXOffset = s.getImageTopLeftX(intersectLeft);

				int otherImageYOffset = s.getImageTopLeftY(intersectTop);

				// check if opaque pixels intersect.

				return doPixelCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage, s.sourceImage,
						intersectWidth, intersectHeight);

			} else {
				// collides!
				return true;
			}
		}
		return false;

	}

	public final boolean collidesWith(Image image, int inp_x,
									  int inp_y, boolean pixelLevel) {

		// check if this Sprite is not visible
		if (!(visible)) {
			return false;
		}

		// if image is null
		// image.getWidth() will throw NullPointerException
		int otherLeft = inp_x;
		int otherTop = inp_y;
		int otherRight = inp_x + image.getWidth();
		int otherBottom = inp_y + image.getHeight();

		int left = x + collisionRectX;
		int top = y + collisionRectY;
		int right = left + collisionRectWidth;
		int bottom = top + collisionRectHeight;

		// first check if the collision rectangles of the two sprites intersect
		if (intersectRect(otherLeft, otherTop, otherRight, otherBottom,
				left, top, right, bottom)) {

			// collision rectangles intersect
			if (pixelLevel) {

				// find intersecting region,

				// we need to check pixel level collision detection.
				// use only the coordinates within the Sprite frame if
				// the collision rectangle is larger than the Sprite
				// frame
				if (this.collisionRectX < 0) {
					left = this.x;
				}
				if (this.collisionRectY < 0) {
					top = this.y;
				}
				if ((this.collisionRectX + this.collisionRectWidth)
						> this.width) {
					right = this.x + this.width;
				}
				if ((this.collisionRectY + this.collisionRectHeight)
						> this.height) {
					bottom = this.y + this.height;
				}

				// recheck if the updated collision area rectangles intersect
				if (!intersectRect(otherLeft, otherTop,
						otherRight, otherBottom,
						left, top, right, bottom)) {

					// if they don't intersect, return false;
					return false;
				}

				// within the collision rectangles
				int intersectLeft = (left < otherLeft) ? otherLeft : left;
				int intersectTop = (top < otherTop) ? otherTop : top;

				// used once, optimize.
				int intersectRight = (right < otherRight)
						? right : otherRight;
				int intersectBottom = (bottom < otherBottom)
						? bottom : otherBottom;

				int intersectWidth = Math.abs(intersectRight - intersectLeft);
				int intersectHeight = Math.abs(intersectBottom - intersectTop);

				// have the coordinates in painter space,
				// need coordinates of top left and width, height
				// in source image of Sprite.

				int thisImageXOffset = getImageTopLeftX(intersectLeft);

				int thisImageYOffset = getImageTopLeftY(intersectTop);

				int otherImageXOffset = intersectLeft - inp_x;
				int otherImageYOffset = intersectTop - inp_y;

				// check if opaque pixels intersect.
				return doPixelCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage, image,
						intersectWidth, intersectHeight);

			} else {
				// collides!
				return true;
			}
		}
		return false;

	}

	private void initializeFrames(Image image, int fWidth,
								  int fHeight, boolean maintainCurFrame) {

		int imageW = image.getWidth();
		int imageH = image.getHeight();

		int numHorizontalFrames = imageW / fWidth;
		int numVerticalFrames = imageH / fHeight;

		sourceImage = image;

		srcFrameWidth = fWidth;
		srcFrameHeight = fHeight;

		numberFrames = numHorizontalFrames * numVerticalFrames;

		frameCoordsX = new int[numberFrames];
		frameCoordsY = new int[numberFrames];

		if (!maintainCurFrame) {
			sequenceIndex = 0;
		}

		if (!customSequenceDefined) {
			frameSequence = new int[numberFrames];
		}

		int currentFrame = 0;

		for (int yy = 0; yy < imageH; yy += fHeight) {
			for (int xx = 0; xx < imageW; xx += fWidth) {

				frameCoordsX[currentFrame] = xx;
				frameCoordsY[currentFrame] = yy;

				if (!customSequenceDefined) {
					frameSequence[currentFrame] = currentFrame;
				}
				currentFrame++;

			}
		}
	}

	private void initCollisionRectBounds() {

		// reset x and y of collision rectangle
		collisionRectX = 0;
		collisionRectY = 0;

		// intialize the collision rectangle bounds to that of the sprite
		collisionRectWidth = this.width;
		collisionRectHeight = this.height;

	}

	private boolean intersectRect(int r1x1, int r1y1, int r1x2, int r1y2,
								  int r2x1, int r2y1, int r2x2, int r2y2) {
		if (r2x1 >= r1x2 || r2y1 >= r1y2 || r2x2 <= r1x1 || r2y2 <= r1y1) {
			return false;
		} else {
			return true;
		}
	}

	private static boolean doPixelCollision(int image1XOffset,
											int image1YOffset,
											int image2XOffset,
											int image2YOffset,
											Image image1, Image image2,
											int width, int height) {

		int numPixels = height * width;

		int[] argbData1 = new int[numPixels];
		int[] argbData2 = new int[numPixels];

		image1.getRGB(argbData1, 0, width, image1XOffset, image1YOffset, width, height);
		image2.getRGB(argbData2, 0, width, image2XOffset, image2YOffset, width, height);

		int x1, x2;
		int xLocalBegin1, xLocalBegin2;

		// the loop counters
		int numIterRows;
		int numIterColumns;

		for (numIterRows = 0, xLocalBegin1 = 0, xLocalBegin2 = 0;
			 numIterRows < height;
			 xLocalBegin1 += width, xLocalBegin2 += width, numIterRows++) {

			for (numIterColumns = 0, x1 = xLocalBegin1, x2 = xLocalBegin2;
				 numIterColumns < width;
				 x1++, x2++, numIterColumns++) {

				if (((argbData1[x1] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA) &&
						((argbData2[x2] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA)) {

					return true;
				}

			} // end for x

		} // end for y

		// worst case!  couldn't find a single colliding pixel!
		return false;
	}

	private int getImageTopLeftX(int x1) {
		int retX = x1 - this.x;

		retX += frameCoordsX[frameSequence[sequenceIndex]];

		return retX;
	}

	private int getImageTopLeftY(int y1) {
		int retY = y1 - this.y;

		retY += frameCoordsY[frameSequence[sequenceIndex]];

		return retY;
	}
}
