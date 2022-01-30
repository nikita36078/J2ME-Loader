/*
 *  Nokia API for MicroEmulator
 *  Copyright (C) 2003 Markus Heberling <markus@heberling.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package com.nokia.mid.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * This class is a placeholder for utility methods. It contains methods for converting standard lcdui classes to Nokia UI classes and vice versa, and a method for creating images that are empty with pixels either transparent or colored, and creating mutable images from encoded image byte arrays.
 */
public class DirectUtils {

	// S40v6 getFont(int) constants
	private static final int SMALL_FONT = 1;
	private static final int MEDIUM_FONT = 2;
	private static final int LARGE_FONT = 3;
	private static final int MEDIUM_BOLD_FONT = 4;
	private static final int DEFAULT_FONT = 5;
	private static final int IDLE_SCREEN_FONT = 6;
	private static final int IDLE_SCREEN_FOCUSED_FONT = 7;

	/**
	 * Converts standard javax.microedition.lcdui.Graphics to DirectGraphics. The returned object refers to the same graphics context. This means that calling draw operations or changing the state, for example, drawing color etc., via the original Graphics reference affect the DirectGraphics object, and vice versa.
	 * <p>
	 * Note that even though the graphics context that the DirectGraphics and Graphics refer to is the same, the object reference returned from this method may or may not be equal compared to the Graphics reference passed to this method. This means that purely casting Graphics object (g) passed in paint method of lcdui Canvas to DirectGraphics may not work ok. The safest way is to always do the conversion with this method.
	 *
	 * @param g Graphics object for which DirectGraphics should be returned
	 * @return the DirectGraphics object based on Graphics
	 */
	public static DirectGraphics getDirectGraphics(Graphics g) {
		return new DirectGraphicsImp(g);
	}

	/**
	 * Creates a mutable image that is decoded from the data stored in the specified byte array at the specified offset and length. The data must be in a self-identifying image file format supported by the implementation, e.g., PNG.
	 * <p>
	 * Note that the semantics of this method are exactly the same as Image.createImage(byte[],int,int) except that the returned image is mutable.
	 *
	 * @param imageData   the array of image data in a supported image format
	 * @param imageOffset the offset of the start of the data in the array
	 * @param imageLength the length of the data in the array
	 * @return the created mutable image
	 */
	public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inMutable = true;
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength, opts);
		if (!bitmap.isMutable()) Log.w(DirectUtils.class.getName(), "createImage: bitmap not is mutable");
		return new Image(bitmap);
	}

	/**
	 * The method will return a newly created mutable Image with the specified dimension and all the pixels of the image defined by the specified ARGB color. The color can contain alpha channel transparency information.
	 *
	 * @param width  the width of the new image, in pixels
	 * @param height the height of the new image, in pixels
	 * @param argb   the initial color for image.
	 * @return the created image
	 */
	public static Image createImage(int width, int height, int argb) {
		return Image.createImage(width, height, argb);
	}

	public static Font getFont(int identifier) {
		switch(identifier) {
			case SMALL_FONT:
				return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			case MEDIUM_FONT:
				return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
			case LARGE_FONT:
				return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
			case MEDIUM_BOLD_FONT:
				return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
			default:
				return Font.getDefaultFont();
		}
	}

	public static javax.microedition.lcdui.Font getFont(int face, int style, int height) {
		return FreeSizeFontInvoker.getFont(face, style, height);
	}

	public static boolean setHeader(Displayable displayable,
									String headerText,
									Image headerImage,
									int headerTextColor,
									int headerBgColor,
									int headerDividerColor) {
		// TODO: 12.04.2021 stub
		return false;
	}
}
