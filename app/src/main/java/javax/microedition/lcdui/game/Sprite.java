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

package javax.microedition.lcdui.game;

import android.graphics.Matrix;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite extends Layer {

	public static final int TRANS_NONE = 0;
	public static final int TRANS_ROT90 = 5;
	public static final int TRANS_ROT180 = 3;
	public static final int TRANS_ROT270 = 6;
	public static final int TRANS_MIRROR = 2;
	public static final int TRANS_MIRROR_ROT90 = 7;
	public static final int TRANS_MIRROR_ROT180 = 1;
	public static final int TRANS_MIRROR_ROT270 = 4;

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

	int dRefX;

	int dRefY;

	int collisionRectX;

	int collisionRectY;

	int collisionRectWidth;

	int collisionRectHeight;

	int t_currentTransformation;

	int t_collisionRectX;

	int t_collisionRectY;

	int t_collisionRectWidth;

	int t_collisionRectHeight;


	public Sprite(Image image) {
		super(image.getWidth(), image.getHeight());

		initializeFrames(image, image.getWidth(), image.getHeight(), false);

		// initialize collision rectangle
		initCollisionRectBounds();

		// current transformation is TRANS_NONE
		setTransformImpl(TRANS_NONE);

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

		// current transformation is TRANS_NONE
		setTransformImpl(TRANS_NONE);

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

		// these fields are set by defining a reference point
		this.dRefX = s.dRefX;
		this.dRefY = s.dRefY;

		// these fields are set when defining a collision rectangle
		this.collisionRectX = s.collisionRectX;
		this.collisionRectY = s.collisionRectY;
		this.collisionRectWidth = s.collisionRectWidth;
		this.collisionRectHeight = s.collisionRectHeight;

		// these fields are set when creating a Sprite from an Image
		this.srcFrameWidth = s.srcFrameWidth;
		this.srcFrameHeight = s.srcFrameHeight;

		// the above fields are used in setTransform()
		// which is why we set them first, then  call setTransformImpl()
		// to set up internally used data structures.
		setTransformImpl(s.t_currentTransformation);

		this.setVisible(s.isVisible());

		this.frameSequence = new int[s.getFrameSequenceLength()];
		this.setFrameSequence(s.frameSequence);
		this.setFrame(s.getFrame());

		this.setRefPixelPosition(s.getRefPixelX(), s.getRefPixelY());

	}

	public void defineReferencePixel(int inp_x, int inp_y) {
		dRefX = inp_x;
		dRefY = inp_y;
	}

	public void setRefPixelPosition(int inp_x, int inp_y) {

		// update x and y
		x = inp_x - getTransformedPtX(dRefX, dRefY,
				t_currentTransformation);
		y = inp_y - getTransformedPtY(dRefX, dRefY,
				t_currentTransformation);

	}

	public int getRefPixelX() {
		return (this.x +
				getTransformedPtX(dRefX, dRefY, this.t_currentTransformation));
	}

	public int getRefPixelY() {
		return (this.y +
				getTransformedPtY(dRefX, dRefY, this.t_currentTransformation));
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
					t_currentTransformation,
					this.x,
					this.y,
					Graphics.TOP | Graphics.LEFT);
		}

	}

	public void setFrameSequence(int sequence[]) {

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

			// computing is the location
			// of the reference pixel in the painter's coordinate system.
			// and then use this to find x and y position of the Sprite
			int oldX = this.x +
					getTransformedPtX(dRefX, dRefY, this.t_currentTransformation);

			int oldY = this.y +
					getTransformedPtY(dRefX, dRefY, this.t_currentTransformation);


			setWidthImpl(frameWidth);
			setHeightImpl(frameHeight);

			initializeFrames(img, frameWidth, frameHeight, maintainCurFrame);

			// initialize collision rectangle
			initCollisionRectBounds();

			// set the new x and y position of the Sprite
			this.x = oldX -
					getTransformedPtX(dRefX, dRefY, this.t_currentTransformation);

			this.y = oldY -
					getTransformedPtY(dRefX, dRefY, this.t_currentTransformation);


			// Calculate transformed sprites collision rectangle
			// and transformed width and height

			computeTransformedBounds(this.t_currentTransformation);

		} else {
			// just reinitialize the animation frames.
			initializeFrames(img, frameWidth, frameHeight, maintainCurFrame);
		}

	}

	public void defineCollisionRectangle(int inp_x, int inp_y,
										 int width, int height) {

		if (width < 0 || height < 0) {
			throw new IllegalArgumentException();
		}

		collisionRectX = inp_x;
		collisionRectY = inp_y;
		collisionRectWidth = width;
		collisionRectHeight = height;

		// call set transform with current transformation to
		// update transformed sprites collision rectangle
		setTransformImpl(t_currentTransformation);
	}

	public void setTransform(int transform) {
		setTransformImpl(transform);
	}

	public final boolean collidesWith(Sprite s, boolean pixelLevel) {

		// check if either of the Sprite's are not visible
		if (!(s.visible && this.visible)) {
			return false;
		}

		// these are package private
		// and can be accessed directly
		int otherLeft = s.x + s.t_collisionRectX;
		int otherTop = s.y + s.t_collisionRectY;
		int otherRight = otherLeft + s.t_collisionRectWidth;
		int otherBottom = otherTop + s.t_collisionRectHeight;

		int left = this.x + this.t_collisionRectX;
		int top = this.y + this.t_collisionRectY;
		int right = left + this.t_collisionRectWidth;
		int bottom = top + this.t_collisionRectHeight;

		// check if the collision rectangles of the two sprites intersect
		if (intersectRect(otherLeft, otherTop, otherRight, otherBottom,
				left, top, right, bottom)) {

			// collision rectangles intersect
			if (pixelLevel) {

				// we need to check pixel level collision detection.
				// use only the coordinates within the Sprite frame if
				// the collision rectangle is larger than the Sprite
				// frame
				if (this.t_collisionRectX < 0) {
					left = this.x;
				}
				if (this.t_collisionRectY < 0) {
					top = this.y;
				}
				if ((this.t_collisionRectX + this.t_collisionRectWidth)
						> this.width) {
					right = this.x + this.width;
				}
				if ((this.t_collisionRectY + this.t_collisionRectHeight)
						> this.height) {
					bottom = this.y + this.height;
				}

				// similarly for the other Sprite
				if (s.t_collisionRectX < 0) {
					otherLeft = s.x;
				}
				if (s.t_collisionRectY < 0) {
					otherTop = s.y;
				}
				if ((s.t_collisionRectX + s.t_collisionRectWidth)
						> s.width) {
					otherRight = s.x + s.width;
				}
				if ((s.t_collisionRectY + s.t_collisionRectHeight)
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

				int thisImageXOffset = getImageTopLeftX(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				int thisImageYOffset = getImageTopLeftY(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				int otherImageXOffset = s.getImageTopLeftX(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				int otherImageYOffset = s.getImageTopLeftY(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				// check if opaque pixels intersect.

				return doPixelCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage,
						this.t_currentTransformation,
						s.sourceImage,
						s.t_currentTransformation,
						intersectWidth, intersectHeight);

			} else {
				// collides!
				return true;
			}
		}
		return false;

	}

	public final boolean collidesWith(TiledLayer t, boolean pixelLevel) {

		// check if either this Sprite or the TiledLayer is not visible
		if (!(t.visible && this.visible)) {
			return false;
		}

		// dimensions of tiledLayer, cell, and
		// this Sprite's collision rectangle

		// these are package private
		// and can be accessed directly
		int tLx1 = t.x;
		int tLy1 = t.y;
		int tLx2 = tLx1 + t.width;
		int tLy2 = tLy1 + t.height;

		int tW = t.getCellWidth();
		int tH = t.getCellHeight();

		int sx1 = this.x + this.t_collisionRectX;
		int sy1 = this.y + this.t_collisionRectY;
		int sx2 = sx1 + this.t_collisionRectWidth;
		int sy2 = sy1 + this.t_collisionRectHeight;

		// number of cells
		int tNumCols = t.getColumns();
		int tNumRows = t.getRows();

		// temporary loop variables.
		int startCol; // = 0;
		int endCol;   // = 0;
		int startRow; // = 0;
		int endRow;   // = 0;

		if (!intersectRect(tLx1, tLy1, tLx2, tLy2, sx1, sy1, sx2, sy2)) {
			// if the collision rectangle of the sprite
			// does not intersect with the dimensions of the entire
			// tiled layer
			return false;
		}

		// so there is an intersection

		// note sx1 < sx2, tLx1 < tLx2, sx2 > tLx1  from intersectRect()
		// use <= for comparison as this saves us some
		// computation - the result will be 0
		startCol = (sx1 <= tLx1) ? 0 : (sx1 - tLx1) / tW;
		startRow = (sy1 <= tLy1) ? 0 : (sy1 - tLy1) / tH;
		// since tLx1 < sx2 < tLx2, the computation will yield
		// a result between 0 and tNumCols - 1
		// subtract by 1 because sx2,sy2 represent
		// the enclosing bounds of the sprite, not the
		// locations in the coordinate system.
		endCol = (sx2 < tLx2) ? ((sx2 - 1 - tLx1) / tW) : tNumCols - 1;
		endRow = (sy2 < tLy2) ? ((sy2 - 1 - tLy1) / tH) : tNumRows - 1;

		if (!pixelLevel) {
			// check for intersection with a non-empty cell,
			for (int row = startRow; row <= endRow; row++) {
				for (int col = startCol; col <= endCol; col++) {
					if (t.getCell(col, row) != 0) {
						return true;
					}
				}
			}
			// worst case! we scanned through entire
			// overlapping region and
			// all the cells are empty!
			return false;
		} else {
			// do pixel level

			// we need to check pixel level collision detection.
			// use only the coordinates within the Sprite frame if
			// the collision rectangle is larger than the Sprite
			// frame
			if (this.t_collisionRectX < 0) {
				sx1 = this.x;
			}
			if (this.t_collisionRectY < 0) {
				sy1 = this.y;
			}
			if ((this.t_collisionRectX + this.t_collisionRectWidth)
					> this.width) {
				sx2 = this.x + this.width;
			}
			if ((this.t_collisionRectY + this.t_collisionRectHeight)
					> this.height) {
				sy2 = this.y + this.height;
			}

			if (!intersectRect(tLx1, tLy1, tLx2, tLy2, sx1, sy1, sx2, sy2)) {
				return (false);
			}

			// we have an intersection between the Sprite and
			// one or more cells of the tiledlayer

			// note sx1 < sx2, tLx1 < tLx2, sx2 > tLx1  from intersectRect()
			// use <= for comparison as this saves us some
			// computation - the result will be 0
			startCol = (sx1 <= tLx1) ? 0 : (sx1 - tLx1) / tW;
			startRow = (sy1 <= tLy1) ? 0 : (sy1 - tLy1) / tH;
			// since tLx1 < sx2 < tLx2, the computation will yield
			// a result between 0 and tNumCols - 1
			// subtract by 1 because sx2,sy2 represent
			// the enclosing bounds of the sprite, not the
			// locations in the coordinate system.
			endCol = (sx2 < tLx2) ? ((sx2 - 1 - tLx1) / tW) : tNumCols - 1;
			endRow = (sy2 < tLy2) ? ((sy2 - 1 - tLy1) / tH) : tNumRows - 1;

			// current cell coordinates
			int cellTop = startRow * tH + tLy1;
			int cellBottom = cellTop + tH;

			// the index of the current tile.
			int tileIndex; // = 0;

			for (int row = startRow; row <= endRow;
				 row++, cellTop += tH, cellBottom += tH) {

				// current cell coordinates
				int cellLeft = startCol * tW + tLx1;
				int cellRight = cellLeft + tW;

				for (int col = startCol; col <= endCol;
					 col++, cellLeft += tW, cellRight += tW) {

					tileIndex = t.getCell(col, row);

					if (tileIndex != 0) {

						// current cell/sprite intersection coordinates
						// in painter coordinate system.
						// find intersecting region,
						int intersectLeft = (sx1 < cellLeft) ? cellLeft : sx1;
						int intersectTop = (sy1 < cellTop) ? cellTop : sy1;

						// used once, optimize.
						int intersectRight = (sx2 < cellRight) ?
								sx2 : cellRight;
						int intersectBottom = (sy2 < cellBottom) ?
								sy2 : cellBottom;

						if (intersectLeft > intersectRight) {
							int temp = intersectRight;
							intersectRight = intersectLeft;
							intersectLeft = temp;
						}

						if (intersectTop > intersectBottom) {
							int temp = intersectBottom;
							intersectBottom = intersectTop;
							intersectTop = temp;
						}

						int intersectWidth = intersectRight - intersectLeft;
						int intersectHeight = intersectBottom - intersectTop;

						int image1XOffset = getImageTopLeftX(intersectLeft,
								intersectTop,
								intersectRight,
								intersectBottom);

						int image1YOffset = getImageTopLeftY(intersectLeft,
								intersectTop,
								intersectRight,
								intersectBottom);

						int image2XOffset = t.tileSetX[tileIndex] +
								(intersectLeft - cellLeft);
						int image2YOffset = t.tileSetY[tileIndex] +
								(intersectTop - cellTop);

						if (doPixelCollision(image1XOffset,
								image1YOffset,
								image2XOffset,
								image2YOffset,
								this.sourceImage,
								this.t_currentTransformation,
								t.sourceImage,
								TRANS_NONE,
								intersectWidth, intersectHeight)) {
							// intersection found with this tile
							return true;
						}
					}
				} // end of for col
			}// end of for row

			// worst case! we scanned through entire
			// overlapping region and
			// no pixels collide!
			return false;
		}

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

		int left = x + t_collisionRectX;
		int top = y + t_collisionRectY;
		int right = left + t_collisionRectWidth;
		int bottom = top + t_collisionRectHeight;

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
				if (this.t_collisionRectX < 0) {
					left = this.x;
				}
				if (this.t_collisionRectY < 0) {
					top = this.y;
				}
				if ((this.t_collisionRectX + this.t_collisionRectWidth)
						> this.width) {
					right = this.x + this.width;
				}
				if ((this.t_collisionRectY + this.t_collisionRectHeight)
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

				int thisImageXOffset = getImageTopLeftX(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				int thisImageYOffset = getImageTopLeftY(intersectLeft,
						intersectTop,
						intersectRight,
						intersectBottom);

				int otherImageXOffset = intersectLeft - inp_x;
				int otherImageYOffset = intersectTop - inp_y;

				// check if opaque pixels intersect.
				return doPixelCollision(thisImageXOffset, thisImageYOffset,
						otherImageXOffset, otherImageYOffset,
						this.sourceImage,
						this.t_currentTransformation,
						image,
						Sprite.TRANS_NONE,
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
											Image image1, int transform1,
											Image image2, int transform2,
											int width, int height) {

		// starting point of comparison
		int startY1;
		// x and y increments
		int xIncr1, yIncr1;

		// .. for image 2
		int startY2;
		int xIncr2, yIncr2;

		int numPixels = height * width;

		int[] argbData1 = new int[numPixels];
		int[] argbData2 = new int[numPixels];

		if (0x0 != (transform1 & INVERTED_AXES)) {

			if (0x0 != (transform1 & Y_FLIP)) {
				xIncr1 = -(height); // - scanlength

				startY1 = numPixels - height; // numPixels - scanlength
			} else {
				xIncr1 = height; // + scanlength

				startY1 = 0;
			}

			if (0x0 != (transform1 & X_FLIP)) {
				yIncr1 = -1;

				startY1 += (height - 1);
			} else {
				yIncr1 = +1;
			}

			image1.getRGB(argbData1, 0, height, // scanlength = height
					image1XOffset, image1YOffset, height, width);

		} else {

			if (0x0 != (transform1 & Y_FLIP)) {

				startY1 = numPixels - width; // numPixels - scanlength

				yIncr1 = -(width); // - scanlength
			} else {
				startY1 = 0;

				yIncr1 = width; // + scanlength
			}

			if (0x0 != (transform1 & X_FLIP)) {
				xIncr1 = -1;

				startY1 += (width - 1);
			} else {
				xIncr1 = +1;
			}

			image1.getRGB(argbData1, 0, width, image1XOffset, image1YOffset, width, height);
		}

		if (0x0 != (transform2 & INVERTED_AXES)) {
			// inverted axes

			if (0x0 != (transform2 & Y_FLIP)) {
				xIncr2 = -(height);

				startY2 = numPixels - height;
			} else {
				xIncr2 = height;

				startY2 = 0;
			}

			if (0x0 != (transform2 & X_FLIP)) {
				yIncr2 = -1;

				startY2 += height - 1;
			} else {
				yIncr2 = +1;
			}

			image2.getRGB(argbData2, 0, height,
					image2XOffset, image2YOffset, height, width);

		} else {

			if (0x0 != (transform2 & Y_FLIP)) {
				startY2 = numPixels - width;

				yIncr2 = -(width);
			} else {
				startY2 = 0;

				yIncr2 = +width;
			}

			if (0x0 != (transform2 & X_FLIP)) {
				xIncr2 = -1;

				startY2 += (width - 1);
			} else {
				xIncr2 = +1;
			}

			image2.getRGB(argbData2, 0, width, image2XOffset, image2YOffset, width, height);
		}

		int x1, x2;
		int xLocalBegin1, xLocalBegin2;

		// the loop counters
		int numIterRows;
		int numIterColumns;

		for (numIterRows = 0, xLocalBegin1 = startY1, xLocalBegin2 = startY2;
			 numIterRows < height;
			 xLocalBegin1 += yIncr1, xLocalBegin2 += yIncr2, numIterRows++) {

			for (numIterColumns = 0, x1 = xLocalBegin1, x2 = xLocalBegin2;
				 numIterColumns < width;
				 x1 += xIncr1, x2 += xIncr2, numIterColumns++) {

				if (((argbData1[x1] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA) &&
						((argbData2[x2] & ALPHA_BITMASK) == FULLY_OPAQUE_ALPHA)) {

					return true;
				}

			} // end for x

		} // end for y

		// worst case!  couldn't find a single colliding pixel!
		return false;
	}

	private int getImageTopLeftX(int x1, int y1, int x2, int y2) {
		int retX = 0;

		switch (this.t_currentTransformation) {

			case TRANS_NONE:
			case TRANS_MIRROR_ROT180:
				retX = x1 - this.x;
				break;

			case TRANS_MIRROR:
			case TRANS_ROT180:
				retX = (this.x + this.width) - x2;
				break;

			case TRANS_ROT90:
			case TRANS_MIRROR_ROT270:
				retX = y1 - this.y;
				break;

			case TRANS_ROT270:
			case TRANS_MIRROR_ROT90:
				retX = (this.y + this.height) - y2;
				break;

			default:
				return retX;
		}

		retX += frameCoordsX[frameSequence[sequenceIndex]];

		return retX;
	}

	private int getImageTopLeftY(int x1, int y1, int x2, int y2) {
		int retY = 0;
		switch (this.t_currentTransformation) {

			case TRANS_NONE:
			case TRANS_MIRROR:
				retY = y1 - this.y;
				break;

			case TRANS_ROT180:
			case TRANS_MIRROR_ROT180:
				retY = (this.y + this.height) - y2;
				break;

			case TRANS_ROT270:
			case TRANS_MIRROR_ROT270:
				retY = x1 - this.x;
				break;

			case TRANS_ROT90:
			case TRANS_MIRROR_ROT90:
				retY = (this.x + this.width) - x2;
				break;

			default:
				return retY;
		}

		retY += frameCoordsY[frameSequence[sequenceIndex]];

		return retY;
	}

	private void setTransformImpl(int transform) {
		// setTransform sets up all transformation related data structures
		// except transforming the current frame's bitmap.

		// x, y, width, height, dRefX, dRefY,
		// collisionRectX, collisionRectY, collisionRectWidth,
		// collisionRectHeight, t_currentTransformation,
		// t_bufferImage

		// The actual transformed frame is drawn at paint time.

		// update top-left corner position
		this.x = this.x +
				getTransformedPtX(dRefX, dRefY, this.t_currentTransformation) -
				getTransformedPtX(dRefX, dRefY, transform);

		this.y = this.y +
				getTransformedPtY(dRefX, dRefY, this.t_currentTransformation) -
				getTransformedPtY(dRefX, dRefY, transform);

		// Calculate transformed sprites collision rectangle
		// and transformed width and height
		computeTransformedBounds(transform);

		// set the current transform to be the one requested
		t_currentTransformation = transform;

	}

	private void computeTransformedBounds(int transform) {
		switch (transform) {

			case TRANS_NONE:

				t_collisionRectX = collisionRectX;
				t_collisionRectY = collisionRectY;
				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;
				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				break;

			case TRANS_MIRROR:

				// flip across vertical

				// NOTE: top left x and y coordinate must reflect the transformation
				// performed around the reference point

				// the X-offset of the reference point from the top left corner
				// changes.
				t_collisionRectX = srcFrameWidth -
						(collisionRectX + collisionRectWidth);

				t_collisionRectY = collisionRectY;
				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;

				// the Y-offset of the reference point from the top left corner
				// remains the same,
				// top left X-co-ordinate changes

				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				break;

			case TRANS_MIRROR_ROT180:

				// flip across horizontal

				// NOTE: top left x and y coordinate must reflect the transformation
				// performed around the reference point

				// the Y-offset of the reference point from the top left corner
				// changes
				t_collisionRectY = srcFrameHeight -
						(collisionRectY + collisionRectHeight);

				t_collisionRectX = collisionRectX;
				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;

				// width and height are as before
				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				// the X-offset of the reference point from the top left corner
				// remains the same.
				// top left Y-co-ordinate changes

				break;

			case TRANS_ROT90:

				// NOTE: top left x and y coordinate must reflect the transformation
				// performed around the reference point

				// the bottom-left corner of the rectangle becomes the
				// top-left when rotated 90.

				// both X- and Y-offset to the top left corner may change

				// update the position information for the collision rectangle

				t_collisionRectX = srcFrameHeight -
						(collisionRectHeight + collisionRectY);
				t_collisionRectY = collisionRectX;

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				// set width and height
				this.width = srcFrameHeight;
				this.height = srcFrameWidth;

				break;

			case TRANS_ROT180:

				// NOTE: top left x and y coordinate must reflect the transformation
				// performed around the reference point

				// width and height are as before

				// both X- and Y- offsets from the top left corner may change

				t_collisionRectX = srcFrameWidth - (collisionRectWidth +
						collisionRectX);
				t_collisionRectY = srcFrameHeight - (collisionRectHeight +
						collisionRectY);

				t_collisionRectWidth = collisionRectWidth;
				t_collisionRectHeight = collisionRectHeight;

				// set width and height
				this.width = srcFrameWidth;
				this.height = srcFrameHeight;

				break;

			case TRANS_ROT270:

				// the top-right corner of the rectangle becomes the
				// top-left when rotated 270.

				// both X- and Y-offset to the top left corner may change

				// update the position information for the collision rectangle

				t_collisionRectX = collisionRectY;
				t_collisionRectY = srcFrameWidth - (collisionRectWidth +
						collisionRectX);

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				// set width and height
				this.width = srcFrameHeight;
				this.height = srcFrameWidth;

				break;

			case TRANS_MIRROR_ROT90:

				// both X- and Y- offset from the top left corner may change

				// update the position information for the collision rectangle

				t_collisionRectX = srcFrameHeight - (collisionRectHeight +
						collisionRectY);
				t_collisionRectY = srcFrameWidth - (collisionRectWidth +
						collisionRectX);

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				// set width and height
				this.width = srcFrameHeight;
				this.height = srcFrameWidth;

				break;

			case TRANS_MIRROR_ROT270:

				// both X- and Y- offset from the top left corner may change

				// update the position information for the collision rectangle

				t_collisionRectY = collisionRectX;
				t_collisionRectX = collisionRectY;

				t_collisionRectHeight = collisionRectWidth;
				t_collisionRectWidth = collisionRectHeight;

				// set width and height
				this.width = srcFrameHeight;
				this.height = srcFrameWidth;
				break;

			default:
				throw new IllegalArgumentException();
		}
	}

	private int getTransformedPtX(int inp_x, int inp_y, int transform) {
		int t_x = 0;
		switch (transform) {

			case TRANS_NONE:
				t_x = inp_x;
				break;
			case TRANS_MIRROR:
				t_x = srcFrameWidth - inp_x - 1;
				break;
			case TRANS_MIRROR_ROT180:
				t_x = inp_x;
				break;
			case TRANS_ROT90:
				t_x = srcFrameHeight - inp_y - 1;
				break;
			case TRANS_ROT180:
				t_x = srcFrameWidth - inp_x - 1;
				break;
			case TRANS_ROT270:
				t_x = inp_y;
				break;
			case TRANS_MIRROR_ROT90:
				t_x = srcFrameHeight - inp_y - 1;
				break;
			case TRANS_MIRROR_ROT270:
				t_x = inp_y;
				break;
			default:
				break;
		}
		return t_x;
	}

	private int getTransformedPtY(int inp_x, int inp_y, int transform) {
		int t_y = 0;
		switch (transform) {

			case TRANS_NONE:
				t_y = inp_y;
				break;
			case TRANS_MIRROR:
				t_y = inp_y;
				break;
			case TRANS_MIRROR_ROT180:
				t_y = srcFrameHeight - inp_y - 1;
				break;
			case TRANS_ROT90:
				t_y = inp_x;
				break;
			case TRANS_ROT180:
				t_y = srcFrameHeight - inp_y - 1;
				break;
			case TRANS_ROT270:
				t_y = srcFrameWidth - inp_x - 1;
				break;
			case TRANS_MIRROR_ROT90:
				t_y = srcFrameWidth - inp_x - 1;
				break;
			case TRANS_MIRROR_ROT270:
				t_y = inp_x;
				break;
			default:
				break;
		}
		return t_y;
	}

	public static Matrix transformMatrix(int transform, float px, float py) {
		Matrix matrix = new Matrix();

		switch (transform) {
			case Sprite.TRANS_ROT90:
				matrix.preRotate(90, px, py);
				break;

			case Sprite.TRANS_ROT180:
				matrix.preRotate(180, px, py);
				break;

			case Sprite.TRANS_ROT270:
				matrix.preRotate(270, px, py);
				break;

			case Sprite.TRANS_MIRROR:
				matrix.preScale(-1, 1, px, py);
				break;

			case Sprite.TRANS_MIRROR_ROT90:
				matrix.preRotate(90, px, py);
				matrix.preScale(-1, 1, px, py);
				break;

			case Sprite.TRANS_MIRROR_ROT180:
				matrix.preRotate(180, px, py);
				matrix.preScale(-1, 1, px, py);
				break;

			case Sprite.TRANS_MIRROR_ROT270:
				matrix.preRotate(270, px, py);
				matrix.preScale(-1, 1, px, py);
				break;
		}

		return matrix;
	}
}
