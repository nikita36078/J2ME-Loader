/*
 * Copyright 2012 Kulikov Dmitriy
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
 
 // authors Andres Navarro and Kulikov Dmitriy

package javax.microedition.lcdui.game;

import android.graphics.Matrix;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;

public class Sprite extends Layer {
 
	public static final int TRANS_NONE = 0;
	public static final int TRANS_MIRROR_ROT180 = 1;
	public static final int TRANS_MIRROR = 2;
	public static final int TRANS_ROT180 = 3;
	public static final int TRANS_MIRROR_ROT270 = 4;
	public static final int TRANS_ROT90 = 5;
	public static final int TRANS_ROT270 = 6;
	public static final int TRANS_MIRROR_ROT90 = 7;

    // current frame index (within the sequence, not the absolut index)
    private int frame;

    // the frame sequence
    // null if the default is used
    private int [] sequence;

    // coordinate of the reference pixel
    private int refX;
    private int refY;

    // number of cols and rows within the image
    private int cols;
    private int rows;

    // the transform aplied to this sprite
    private int transform;

    // the image containg the frames
    private Image img;

    // the collision rectangle
    private int collX;
    private int collY;
    private int collWidth;
    private int collHeight;

    // arrays for the collision detection at pixel level
    private int []rgbData;
    private int []rgbDataAux;

    public Sprite(Image img) {
        this(img, img.getWidth(), img.getHeight());
    }

    public Sprite(Image img, int frameWidth, int frameHeight) {
        // initial state is visible, positioned at 0, 0
        // with a bound rectangle the same as the frame
        super(0, 0, frameWidth, frameHeight, true);

        // implicit check for null img
        if (img.getWidth() % frameWidth != 0 ||
			img.getHeight() % frameHeight != 0)
            throw new IllegalArgumentException();
        this.img = img;
        cols = img.getWidth() / frameWidth;
        rows = img.getHeight() / frameHeight;
        collX = collY = 0;
        collWidth = frameWidth;
        collHeight = frameHeight;
    }

    public Sprite(Sprite otherSprite) {
        // copy the otherSprite
        super(otherSprite.getX(), otherSprite.getY(), 
			  otherSprite.getWidth(), otherSprite.getHeight(),
			  otherSprite.isVisible());
        this.frame = otherSprite.frame;
        this.sequence = otherSprite.sequence;
        this.refX = otherSprite.refX;
        this.refY = otherSprite.refY;
        this.cols = otherSprite.cols;
        this.rows = otherSprite.rows;
        this.transform = otherSprite.transform;
        this.img = otherSprite.img;
        this.collX = otherSprite.collX;
        this.collY = otherSprite.collY;
        this.collWidth = otherSprite.collWidth;
        this.collHeight = otherSprite.collHeight;
    }

    public final boolean collidesWith(Image image, int iX, int iY, boolean pixelLevel) {
        if (image == null)
            throw new IllegalArgumentException();

        // only check collision if visible
        if (!this.isVisible())
			return false;

        if (pixelLevel)
            return collidesWithPixelLevel(image, iX, iY);
        else
            return collidesWith(image, iX, iY);
    }



    /*public final boolean collidesWith(TiledLayer layer, boolean pixelLevel) {
        int sX, sY;
        int sW, sH;

        if (layer == null) {
            throw new NullPointerException();
        }

        // only check collision if visible
        if (!this.isVisible())
			return false;

        // only check collision if both are visible
        if (!layer.isVisible() || !this.isVisible())
            return false;

        if (pixelLevel) // second and third parameters are dont care
            return collidesWithPixelLevel(layer, 0, 0);
        else 
            return collidesWith(layer, 0, 0);
    }*/

    public final boolean collidesWith(Sprite otherSprite, boolean pixelLevel) {
        int sX, sY;
        int sW, sH;

        if (otherSprite == null) {
            throw new NullPointerException();
        }

        // only check collision if both are visible
        if (!otherSprite.isVisible() || !this.isVisible())
            return false;

        if (pixelLevel) // second and third parameters are dont care
            return collidesWithPixelLevel(otherSprite, 0, 0);
        else 
            return collidesWith(otherSprite, 0, 0);
    }

    public void defineReferencePixel(int x, int y) {
        refX = x;
        refY = y;
    }

    public int getRefPixelX() {
        return getX() + refX;
    }

    public int getRefPixelY() {
        return getY() + refY;
    }

    public void setRefPixelPosition(int x, int y) {
        int curRefX, curRefY;
        int width = getWidth();
        int height = getHeight();

        switch(transform) {
            case TRANS_NONE:
                curRefX = refX;
                curRefY = refY;
                break;
            case TRANS_MIRROR_ROT180:
                curRefX = width - refX;
                curRefY = height - refY;
                break;
            case TRANS_MIRROR:
                curRefX = width - refX;
                curRefY = refY;
                break;
            case TRANS_ROT180:
                curRefX = refX;
                curRefY = height - refY;
                break;
            case TRANS_MIRROR_ROT270:
                curRefX = height - refY;
                curRefY = refX;
                break;
            case TRANS_ROT90:
                curRefX = height - refY;
                curRefY = width - refX;
                break;
            case TRANS_ROT270:
                curRefX = refY;
                curRefY = refX;
                break;
            case TRANS_MIRROR_ROT90:
                curRefX = refY;
                curRefY = width - refX;
                break;
            default: // cant really happen, but the return keeps the
				// compiler happy (otherwise it'll report variable
				// may not be initialized)
                return;
        }

        setPosition(x - curRefX, y - curRefY);
    }

    public void defineCollisionRectangle(int x, int y, int width, int height) {
        if (width < 0 || height < 0)
            throw new IllegalArgumentException();
        collX = x;
        collY = y;
        collWidth = width;
        collHeight = height;
    }

    public void setFrameSequence(int []sequence) {
        if (sequence == null) {
            // return to default sequence
            this.sequence = null;
            return;
        }

        int max = (rows*cols)-1;

        int l = sequence.length;

        if (l == 0)
            throw new IllegalArgumentException();

        for (int i = 0; i < l; i++) {
            int value = sequence[i];
            if (value > max || value < 0)
                throw new ArrayIndexOutOfBoundsException();
        }

        this.sequence = sequence;
        // the frame number has to be reseted
        this.frame = 0;
    }

    public final int getFrame() {
		return frame;
    }

    public int getFrameSequenceLength() {
    	return (sequence == null) ? rows*cols : sequence.length; 
    }

    public void setFrame(int frame) {
        int l = (sequence == null)? rows*cols : sequence.length; 
        if (frame < 0 || frame >= l) {
            throw new IndexOutOfBoundsException();
        }
        this.frame = frame;
    }

    public void nextFrame() {
        if (frame == ((sequence == null)? rows*cols : sequence.length) - 1)
            frame = 0;
        else
            frame++;
    }

    public void prevFrame() {
        if (frame == 0)
            frame = ((sequence == null)? rows*cols : sequence.length) - 1;
        else
            frame--;
    }

    public void setImage(Image img, int frameWidth, int frameHeight) {
    	synchronized (this) {
	        int oldW = getWidth();
	        int oldH = getHeight();
	        int newW = img.getWidth();
	        int newH = img.getHeight();

	        // implicit size check
	        setSize(frameWidth, frameHeight);

	        if (img.getWidth() % frameWidth != 0 ||
				img.getHeight() % frameHeight != 0)
	            throw new IllegalArgumentException();
	        this.img = img;

	        int oldFrames = cols*rows;
	        cols = img.getWidth() / frameWidth;
	        rows = img.getHeight() / frameHeight;

	        if (rows*cols < oldFrames) {
	            // there are fewer frames
	            // reset frame number and sequence
	            sequence = null;
	            frame = 0;
	        }

	        if (frameWidth != getWidth() || frameHeight != getHeight()) {
	            // size changed
	            // reset collision rectangle and collision detection array
	            defineCollisionRectangle(0, 0, frameWidth, frameHeight);
	            rgbData = rgbDataAux = null;

	            // if necessary change position to keep the reference pixel in place

	            if (transform != TRANS_NONE) {
	                int dx, dy;
	                switch(transform) {
	                    case TRANS_MIRROR_ROT180:
	                        dx = newW - oldW;
	                        dy = newH - oldH;
	                        break;
	                    case TRANS_MIRROR:
	                        dx = newW - oldW;
	                        dy = 0;
	                        break;
	                    case TRANS_ROT180:
	                        dx = 0;
	                        dy = newH - oldH;
	                        break;
	                    case TRANS_MIRROR_ROT270:
	                        dx = newH - oldH;
	                        dy = 0;
	                        break;
	                    case TRANS_ROT90:
	                        dx = newH - oldH;
	                        dy = newW - oldW;
	                        break;
	                    case TRANS_ROT270:
	                        dx = 0;
	                        dy = 0;
	                        break;
	                    case TRANS_MIRROR_ROT90:
	                        dx = 0;
	                        dy = newW - oldW;
	                        break;
	                    default: // cant really happen, but the return keeps the
							// compiler happy (otherwise it'll report variable
							// may not be initialized)
	                        return;
	                }
	                // now change position to keep the refPixel in place
	                move(dx, dy);
	            }	                
	        }
    	}
    }

    public final void paint(Graphics g) {
        if (!isVisible())
            return;

        int f = (sequence == null)? frame : sequence[frame];
        int w = getWidth();
        int h = getHeight();
        int fx = w * (f % cols);
        int fy = h * (f / cols);        

        g.drawRegion(img, fx, fy, w, h, transform, getX(), getY(), Graphics.TOP | Graphics.LEFT);
    }

    public int getRawFrameCount() {
        return cols * rows;
    }

    public void setTransform (int transform) {
        if (this.transform == transform)
            return;

        int width = getWidth();
        int height = getHeight();
        int currentTransform = this.transform;

        // calculate the coordinates of refPixel in the new transform
        // relative to x, y

        int newRefX, newRefY;

        switch(transform) {
            case TRANS_NONE:
                newRefX = refX;
                newRefY = refY;
                break;
            case TRANS_MIRROR_ROT180:
                newRefX = width - refX;
                newRefY = height - refY;
                break;
            case TRANS_MIRROR:
                newRefX = width - refX;
                newRefY = refY;
                break;
            case TRANS_ROT180:
                newRefX = refX;
                newRefY = height - refY;
                break;
            case TRANS_MIRROR_ROT270:
                newRefX = height - refY;
                newRefY = refX;
                break;
            case TRANS_ROT90:
                newRefX = height - refY;
                newRefY = width - refX;
                break;
            case TRANS_ROT270:
                newRefX = refY;
                newRefY = refX;
                break;
            case TRANS_MIRROR_ROT90:
                newRefX = refY;
                newRefY = width - refX;
                break;
            default:
                throw new IllegalArgumentException();
        }

        // calculate the coordinates of refPixel in the current transform
        // relative to x, y

        int curRefX, curRefY;

        switch(currentTransform) {
            case TRANS_NONE:
                curRefX = refX;
                curRefY = refY;
                break;
            case TRANS_MIRROR_ROT180:
                curRefX = width - refX;
                curRefY = height - refY;
                break;
            case TRANS_MIRROR:
                curRefX = width - refX;
                curRefY = refY;
                break;
            case TRANS_ROT180:
                curRefX = refX;
                curRefY = height - refY;
                break;
            case TRANS_MIRROR_ROT270:
                curRefX = height - refY;
                curRefY = refX;
                break;
            case TRANS_ROT90:
                curRefX = height - refY;
                curRefY = width - refX;
                break;
            case TRANS_ROT270:
                curRefX = refY;
                curRefY = refX;
                break;
            case TRANS_MIRROR_ROT90:
                curRefX = refY;
                curRefY = width - refX;
                break;
            default: // cant really happen, but the return keeps the
				// compiler happy (otherwise it'll report variable
				// may not be initialized)
                return;
        }

        // now change position to keep the refPixel in place
        move(curRefX - newRefX, curRefY - newRefY);
        this.transform = transform;
    }


    /**
     * Helper methods that check for collisions
     * They are at the end of the file because of the 
     * length of the code
     *
     * For both methods, the second and third parameters 
     * are significant only if o is an Image, 
     * otherwise they are ignored
     */
    private synchronized boolean collidesWith(Object o, 
											  int oX, int oY) {

        int tX = 0, tY = 0, tW = 0, tH = 0;
        int oW = 0, oH = 0;

        Sprite t = this;
        boolean another = true;


        while (another) {
            int sX, sY, sW, sH;

            int cX = t.collX;
            int cY = t.collY;
            int cW = t.collWidth;
            int cH = t.collHeight;

            // if there is a zero in a dimension
            // then it cannot intersect with anything!
            if (cW == 0 || cH == 0) {
                return false;
            }

            switch(t.transform) {
                case TRANS_NONE:
                    sX = t.getX() + cX;
                    sY = t.getY() + cY;
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_MIRROR_ROT180:
                    sX = t.getX() + cX;
                    sY = t.getY() + (t.getHeight() - cY - 1) - cH;
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_MIRROR:
                    sX = t.getX() + (t.getWidth() - cX - 1) - cW;
                    sY = t.getY() + cY;
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_ROT180:
                    sX = t.getX() + (t.getWidth() - cX - 1) - cW;
                    sY = t.getY() + (t.getHeight() - cY - 1) - cH; 
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_MIRROR_ROT270:
                    sX = t.getX() + cY;
                    sY = t.getY() + cX;
                    sW = cH;
                    sH = cW;
                    break;
                case TRANS_ROT90:
                    sX = t.getX() + (t.getHeight() - cY - 1) - cH;
                    sY = t.getY() + cX;
                    sW = cH;
                    sH = cW;
                    break;
                case TRANS_MIRROR_ROT90:
                    sX = t.getX() + (t.getHeight() - cY - 1) - cH;
                    sY = t.getY() + (t.getWidth() - cX - 1) - cW;
                    sW = cH;
                    sH = cW;
                    break;
                case TRANS_ROT270:
                    sX = t.getX() + cY;
                    sY = t.getY() + (t.getWidth() - cX - 1) - cW;
                    sW = cH;
                    sH = cW;
                    break;
                default: // cant really happen, but the return keeps the
					// compiler happy (otherwise it'll report variable
					// may not be initialized)
                    return false;
            }

            if (o != t) {
                tX = sX;
                tY = sY;
                tW = sW;
                tH = sH;
                if (o instanceof Sprite) {
                    // two sprites first round
                    // another = true;
                    t = (Sprite) o;
                }/* else if (o instanceof TiledLayer) {
                    another = false;
                    TiledLayer layer = (TiledLayer) o;
                    oX = layer.getX();
                    oY = layer.getY();
                    oW = layer.getWidth();
                    oH = layer.getHeight();
                }*/ else { // o instanceof lcdui.Image
                    another = false;
                    Image img = (Image) o;
                    oW = img.getWidth();
                    oH = img.getHeight();
                }
            } else {
                another = false;
                // two sprites
                // second round
                oX = sX;
                oY = sY;
                oW = sW;
                oH = sH;
            }
        }

        // if there is no intersection
        // we know there is no collision
        if (tX > oX && tX >= oX + oW)
            return false;
        else if (tX < oX && tX + tW <= oX)
            return false;
        else if (tY > oY && tY >= oY + oH)
            return false;
        else if (tY < oY && tY + tH <= oY)
            return false;

        /*if (o instanceof TiledLayer) {
            // if o is a tiledLayer then
            // it is possible to have not a
            // collision if every intersection tile
            // has a zero value 
            TiledLayer layer = (TiledLayer) o;
            // this is the intersection of the two rectangles
            int rX, rY, rW, rH;

            if (oX > tX) {
                rX = oX;
                rW = ((oX + oW < tX + tW)? oX + oW : tX + tW) - rX;
            } else {
                rX = tX;
                rW = ((tX + tW < oX + oW)? tX + tW : oX + oW) - rX;
            }
            if (oY > tY) {
                rY = oY;
                rH = ((oY + oH < tY + tH)? oY + oH : tY + tH) - rY;
            } else {
                rY = tY;
                rH = ((tY + tH < oY + oH)? tY + tH : oY + oH) - rY;
            }

            Image img = layer.img;

            int lW = layer.getCellWidth();
            int lH = layer.getCellHeight();

            int minC = (rX - oX) / lW;
            int minR = (rY - oY) / lH;
            int maxC = (rX - oX + rW - 1) / lW;
            int maxR = (rY - oY + rH - 1) / lH;

            // travel across all cells in the collision
            // rectangle
            for (int row = minR; row <= maxR; row++) {
                for (int col = minC; col <= maxC; col++) {
                    int cell = layer.getCell(col, row);
                    // if cell is animated get current
                    // associated static tile
                    if (cell < 0)
                        cell = layer.getAnimatedTile(cell);

                    if (cell != 0)
                        return true;
                }
            }

            // if no non zero cell was found
            // there is no collision
            return false;
        } else {*/
            // if the other object is an image or sprite
            // collision happened
            return true;
        //}
    }    

    private synchronized boolean collidesWithPixelLevel(Object o, 
														int oX, int oY) {


        boolean another = true;
        Sprite t = this;

        // the compiler bitchs if we dont initialize this
        int tX = 0, tY = 0, tW = 0, tH = 0;
        int oW = 0, oH = 0;

        while (another) {
            // first calculate the actual rectangle we must check 
            // for this sprite
            // this are for the reduced collision rectangle
            int cX, cY, cW, cH;
            // this are for the intersection of the bounds rectangle 
            // and collision rectangle, taking into account
            // position and transform
            int sX, sY, sW, sH;



            // take the collision rectangle in account
            // but since the pixels outside the frame are
            // considered transparent, first we have to
            // take the intersection between the collision
            // rectangle and the frame bounds

            if (t.collX >= t.getWidth() || t.collX + t.collWidth <= 0 
				|| t.collY >= t.getHeight() || t.collY + t.collHeight <= 0)
            // collision rectangle outside frame bounds
                return false;

            cX = (t.collX >= 0)? t.collX : 0;
            cY = (t.collY >= 0)? t.collY : 0;
            cW = (t.collX + t.collWidth < t.getWidth())? t.collX + t.collWidth - cX : t.getWidth() - cX;
            cH = (t.collY + t.collHeight < t.getHeight())? t.collY + t.collHeight - cY : t.getHeight() - cY;

            switch(t.transform) {
                case TRANS_NONE:
                    sX = t.getX() + cX;
                    sY = t.getY() + cY;
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_MIRROR_ROT180:
                    sX = t.getX() + cX;
                    sY = t.getY() + (t.getHeight() - cY - 1) - cH;
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_MIRROR:
                    sX = t.getX() + (t.getWidth() - cX - 1) - cW;
                    sY = t.getY() + cY;
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_ROT180:
                    sX = t.getX() + (t.getWidth() - cX - 1) - cW;
                    sY = t.getY() + (t.getHeight() - cY - 1) - cH; 
                    sW = cW;
                    sH = cH;
                    break;
                case TRANS_MIRROR_ROT270:
                    sX = t.getX() + cY;
                    sY = t.getY() + cX;
                    sW = cH;
                    sH = cW;
                    break;
                case TRANS_ROT90:
                    sX = t.getX() + (t.getHeight() - cY) - cH;
                    sY = t.getY() + cX;
                    sW = cH;
                    sH = cW;
                    break;
                case TRANS_MIRROR_ROT90:
                    sX = t.getX() + (t.getHeight() - cY) - cH;
                    sY = t.getY() + (t.getWidth() - cX) - cW;
                    sW = cH;
                    sH = cW;
                    break;
                case TRANS_ROT270:
                    sX = t.getX() + cY;
                    sY = t.getY() + (t.getWidth() - cX) - cW;
                    sW = cH;
                    sH = cW;
                    break;
                default: // cant really happen, but the return keeps the
					// compiler happy (otherwise it'll report variable
					// may not be initialized)
                    return false;
            }

            if (o != t) {
                tX = sX;
                tY = sY;
                tW = sW;
                tH = sH;
                if (o instanceof Sprite) {
                    // two sprites first round
                    // another = true;
                    t = (Sprite) o;
                }/* else if (o instanceof TiledLayer) {
                    another = false;
                    TiledLayer layer = (TiledLayer) o;
                    oX = layer.getX();
                    oY = layer.getY();
                    oW = layer.getWidth();
                    oH = layer.getHeight();
                }*/ else { // o instanceof lcdui.Image
                    another = false;
                    Image img = (Image) o;
                    oW = img.getWidth();
                    oH = img.getHeight();
                }
            } else {
                another = false;
                // two sprites
                // second round
                oX = sX;
                oY = sY;
                oW = sW;
                oH = sH;
            }
        }

        // if there is no intersection
        // we know there is no collision
        if (tX > oX && tX >= oX + oW)
            return false;
        else if (tX < oX && tX + tW <= oX)
            return false;
        else if (tY > oY && tY >= oY + oH)
            return false;
        else if (tY < oY && tY + tH <= oY)
            return false;

        // variables keep popping out of nowhere...
        // this is the intersection of the two rectangles
        int rX, rY, rW, rH;


        if (oX > tX) {
            rX = oX;
            rW = ((oX + oW < tX + tW)? oX + oW : tX + tW) - rX ;
        } else {
            rX = tX;
            rW = ((tX + tW < oX + oW)? tX + tW : oX + oW) - rX;
        }
        if (oY > tY) {
            rY = oY;
            rH = ((oY + oH < tY + tH)? oY + oH : tY + tH) - rY ;
        } else {
            rY = tY;
            rH = ((tY + tH < oY + oH)? tY + tH : oY + oH) - rY;
        }

        // ...and a lot more..
        int tColIncr = 0, tRowIncr = 0, tOffset = 0;
        int oColIncr = 0, oRowIncr = 0, oOffset = 0;

        int f = (sequence == null)? frame : sequence[frame];

        int fW = getWidth();
        int fH = getHeight();
        int fX = fW * (f % rows);
        int fY = fH * (f / rows);

        if (rgbData == null) {
            rgbData = new int[fW*fH];
            rgbDataAux = new int[fW*fH];
        }

        t = this;
        another = true;
        int[] tRgbData = this.rgbData;

        while (another) {
            int sOffset;
            int sColIncr;
            int sRowIncr;

            switch(t.transform) {
                case TRANS_NONE:
                    t.img.getRGB(tRgbData, 0, rW, fX + rX - t.getX(), fY + rY - t.getY(), rW, rH); 
                    sOffset = 0;
                    sColIncr = 1;
                    sRowIncr = 0;
                    break;
                case TRANS_ROT180:
                    t.img.getRGB(tRgbData, 0, rW, fX + fW - (rX - t.getX()) - rW - 1, fY + fH - (rY - t.getY()) - rH - 1, rW, rH); 
                    sOffset = (rH * rW) - 1;
                    sColIncr = -1;
                    sRowIncr =  0;
                    break;
                case TRANS_MIRROR:
                    t.img.getRGB(tRgbData, 0, rW, fX + fW - (rX - t.getX()) - rW - 1, fY + rY - t.getY(), rW, rH); 
                    sOffset = rW - 1;
                    sColIncr = -1;
                    sRowIncr =  rW << 1;
                    break;
                case TRANS_MIRROR_ROT180:
                    t.img.getRGB(tRgbData, 0, rW, fX + rX - t.getX(), fY + fH - (rY - t.getY()) - rH - 1, rW, rH); 
                    sOffset = (rH - 1) * rW;
                    sColIncr = 1;
                    sRowIncr =  -(rW << 1);
                    break;
                case TRANS_ROT90:
                    t.img.getRGB(tRgbData, 0, rH, fX + rY - t.getY(), fY + fH - (rX - t.getX()) - rW, rH, rW); 
                    sOffset = (rW - 1) * rH;
                    sColIncr = -rH;
                    sRowIncr = (rH * rW) + 1;
                    break;
                case TRANS_MIRROR_ROT90:
                    t.img.getRGB(tRgbData, 0, rH, fX + fW - (rY - t.getY()) - rH, fY + fH - (rX - t.getX()) - rW, rH, rW); 
                    sOffset = (rH * rW) - 1;
                    sColIncr = -rH;
                    sRowIncr = (rH * rW) - 1;
                    break;
                case TRANS_MIRROR_ROT270:
                    t.img.getRGB(tRgbData, 0, rH, fX + rY - t.getY(), fY + rX - t.getX(), rH, rW); 
                    sOffset = 0;
                    sColIncr = rH;
                    sRowIncr = -(rH * rW) + 1;
                    break;
                case TRANS_ROT270:
                    t.img.getRGB(tRgbData, 0, rH, fX + fW - (rY - t.getY()) - rH, fY + rX - t.getX(), rH, rW); 
                    sOffset = rH - 1;
                    sColIncr = rH;
                    sRowIncr =  -(rH * rW) - 1;
                    break;
                default: // cant really happen, but the return keeps the
					// compiler happy (otherwise it'll report variable
					// may not be initialized)
                    return false;
            }

            if (o != t) {
                tOffset = sOffset;
                tRowIncr = sRowIncr;
                tColIncr = sColIncr;

                if (o instanceof Sprite) {
                    // two sprites first round
                    // another = true;
                    t = (Sprite) o;
                    tRgbData = this.rgbDataAux;

                    f = (t.sequence == null)? t.frame : t.sequence[t.frame];

                    fW = t.getWidth();
                    fH = t.getHeight();
                    fX = fW * (f % t.rows);
                    fY = fH * (f / t.rows);
                }/* else if (o instanceof TiledLayer) {
                    another = false;
                    TiledLayer layer = (TiledLayer) o;
                    Image img = layer.img;

                    oOffset = 0;
                    oColIncr = 1;
                    oRowIncr = 0;

                    int lW = layer.getCellWidth();
                    int lH = layer.getCellHeight();

                    int minC = (rX - oX) / lW;
                    int minR = (rY - oY) / lH;
                    int maxC = (rX - oX + rW - 1) / lW;
                    int maxR = (rY - oY + rH - 1) / lH;

                    // travel across all cells in the collision
                    // rectangle
                    for (int row = minR; row <= maxR; row++) {
                        for (int col = minC; col <= maxC; col++) {
                            int cell = layer.getCell(col, row);
                            // if cell is animated get current
                            // associated static tile
                            if (cell < 0)
                                cell = layer.getAnimatedTile(cell);

                            int minX = (col == minC)? (rX - oX) % lW : 0;
                            int minY = (row == minR)? (rY - oY) % lH : 0;
                            int maxX = (col == maxC)? (rX + rW - oX - 1) % lW : lW-1;
                            int maxY = (row == maxR)? (rY + rH - oY - 1) % lH : lH-1;


                            int c = (row - minR) * lH * rW + (col - minC) * lW -
								((col == minC)? 0 : (rX - oX) % lW) -
								((row == minR)? 0 : (rY - oY) % lH) * rW;

                            // if cell is invisible we should still set
                            // all points as transparent to prevent
                            // fake positives caused by residual data
                            // on the rgb array
                            if (cell == 0) {

                                for (int y = minY; y <= maxY; y++, 
								c += rW - (maxX - minX + 1)) {
                                    for (int x = minX; x <= maxX; x++, c++) {
                                        rgbDataAux[c] = 0;
                                    }
                                }
                            } else {
                                // make cell 0-based
                                cell--;

                                int imgCols = img.getWidth() / layer.getCellWidth();
                                int xSrc = lW * (cell % imgCols);
                                int ySrc = (cell / imgCols) * lH;
                                img.getRGB(rgbDataAux, c, rW, xSrc + minX, 
										   ySrc + minY, maxX - minX + 1, 
										   maxY - minY + 1);

                            }
                        }
                    }
                }*/ else { // o instanceof lcdui.Image
                    another = false;
                    Image img = (Image) o;
                    // get the image rgb data, and the increments
                    img.getRGB(rgbDataAux, 0, rW, rX - oX, rY - oY, rW, rH);
                    oOffset = 0;
                    oColIncr = 1;
                    oRowIncr = 0;
                }
            } else {
                // two sprites
                // second round, exit the loop
                another = false;
                oOffset = sOffset;
                oRowIncr = sRowIncr;
                oColIncr = sColIncr;
            }
        }

        for (int row = 0; row < rH; row++, tOffset += tRowIncr, oOffset += oRowIncr) {
            for (int col = 0; col < rW; col++, tOffset += tColIncr, oOffset += oColIncr) {
                int rgb = rgbData[tOffset];
                int rgbA = rgbDataAux[oOffset];
                // look for two opaque pixels
                if (((rgb & rgbA) >> 24) == -1)
                    return true;
            }
        }
        return false;
    }
	
	/**
	 * Получить матрицу для указанного преобразования.
	 *
	 * @param matrix матрица, в которую нужно добавить преобразование, или null для создания новой матрицы
	 * @param transform преобразование
	 * @param px координаты центра преобразования
	 * @param py координаты центра преобразования
	 * @return матрица с добавленным преобразованием
	 */
	public static Matrix transformMatrix(Matrix matrix, int transform, float px, float py)
	{
		if(matrix == null)
		{
			matrix = new Matrix();
		}
		
		switch(transform)
		{
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
				matrix.preScale(-1, 1, px, py);
				matrix.preRotate(90, px, py);
				break;
				
			case Sprite.TRANS_MIRROR_ROT180:
				matrix.preScale(-1, 1, px, py);
				matrix.preRotate(180, px, py);
				break;
				
			case Sprite.TRANS_MIRROR_ROT270:
				matrix.preScale(-1, 1, px, py);
				matrix.preRotate(270, px, py);
				break;
		}
		
		return matrix;
	}
}
