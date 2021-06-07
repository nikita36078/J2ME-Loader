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

package javax.microedition.lcdui.overlay;

import android.graphics.RectF;

import javax.microedition.lcdui.Canvas;

public interface Overlay extends Layer {
	/**
	 * Target the overlay at the specified Canvas
	 *
	 * @param canvas Canvas, which, if necessary, should handle key pressings and pointer movement
	 */
	void setTarget(Canvas canvas);

	/**
	 * Called when resizing real (for example, after rotation) or virtual screens.
	 *
	 * @param screen        the size of the device real screen
	 * @param virtualScreen the size of the virtual screen that is available to the midlet
	 */
	void resize(RectF screen, RectF virtualScreen);

	/**
	 * Called when the hardware key is pressed for the first time.
	 *
	 * @param keyCode the code of the pressed key
	 * @return true, if the pressing is processed here and no further handling is necessary
	 */
	boolean keyPressed(int keyCode);

	/**
	 * Called when the hardware key is pressed again (2, 3, etc.).
	 *
	 * @param keyCode the code of the pressed key
	 * @return true, if the pressing is processed here and no further handling is necessary
	 */
	boolean keyRepeated(int keyCode);

	/**
	 * Called when the hardware key is released.
	 *
	 * @param keyCode the code of the pressed key
	 * @return true, if the pressing is processed here and no further handling is necessary
	 */
	boolean keyReleased(int keyCode);

	/**
	 * Called when the pointer touches the screen.
	 *
	 * @param pointer index of the pointer (always 0 if there is one pointer;
	 *                may be greater than 0 if the device supports multitouch)
	 * @param x       the horizontal coordinate of the pointer touch point on the screen
	 * @param y       the vertical coordinate of the pointer touch point on the screen
	 * @return true, if the touch is processed here and no further handling is necessary
	 */
	boolean pointerPressed(int pointer, float x, float y);

	/**
	 * Called when the pointer moves around the screen.
	 *
	 * @param pointer index of the pointer (always 0 if there is one pointer;
	 *                may be greater than 0 if the device supports multitouch)
	 * @param x       the horizontal coordinate of the pointer touch point on the screen
	 * @param y       the vertical coordinate of the pointer touch point on the screen
	 * @return true, if the movement is processed here and no further handling is necessary
	 */
	boolean pointerDragged(int pointer, float x, float y);

	/**
	 * Called when the pointer is released.
	 *
	 * @param pointer index of the pointer (always 0 if there is one pointer;
	 *                may be greater than 0 if the device supports multitouch)
	 * @param x       the horizontal coordinate of the pointer touch point on the screen
	 * @param y       the vertical coordinate of the pointer touch point on the screen
	 * @return true, if the touch is processed here and no further handling is necessary
	 */
	boolean pointerReleased(int pointer, float x, float y);

	/**
	 * Show overlay.
	 * Called by the Canvas at the first touch of the screen with a pointer.
	 */
	void show();

	/**
	 * Hide overlay.
	 * Called by the Canvas when the last pointer is released from the screen.
	 */
	void hide();


	/**
	 * Cancel input events.
	 * Called when the target has lost focus.
	 */
	void cancel();
}