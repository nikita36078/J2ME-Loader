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
package javax.microedition.lcdui.event;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Event;
import javax.microedition.util.ArrayStack;

public class CanvasEvent extends Event {

    private static ArrayStack<CanvasEvent> recycled = new ArrayStack();

    public static final int KEY_PRESSED = 0,
            KEY_REPEATED = 1,
            KEY_RELEASED = 2,
            POINTER_PRESSED = 3,
            POINTER_DRAGGED = 4,
            POINTER_RELEASED = 5,
            SHOW_NOTIFY = 6,
            HIDE_NOTIFY = 7,
            SIZE_CHANGED = 8;

    private static int[] enqueued = new int[9];

    private Canvas canvas;
    private int eventType;

    private int keyCode;

    private int pointer;
    private float x, y;

    private int width;
    private int height;

    public static Event getInstance(Canvas canvas, int eventType) {
        CanvasEvent instance = recycled.pop();

        if (instance == null) {
            instance = new CanvasEvent();
        }

        instance.canvas = canvas;
        instance.eventType = eventType;

        return instance;
    }

    public static Event getInstance(Canvas canvas, int eventType, int keyCode) {
        CanvasEvent instance = recycled.pop();

        if (instance == null) {
            instance = new CanvasEvent();
        }

        instance.canvas = canvas;
        instance.eventType = eventType;
        instance.keyCode = keyCode;

        return instance;
    }

    public static Event getInstance(Canvas canvas, int eventType, int pointer, float x, float y) {
        CanvasEvent instance = recycled.pop();

        if (instance == null) {
            instance = new CanvasEvent();
        }

        instance.canvas = canvas;
        instance.eventType = eventType;
        instance.pointer = pointer;
        instance.x = x;
        instance.y = y;

        return instance;
    }

    public static Event getInstance(Canvas canvas, int eventType, int width, int height) {
        CanvasEvent instance = recycled.pop();

        if (instance == null) {
            instance = new CanvasEvent();
        }

        instance.canvas = canvas;
        instance.eventType = eventType;
        instance.width = width;
        instance.height = height;

        return instance;
    }

    public void process() {
        switch (eventType) {
            case KEY_PRESSED:
                canvas.keyPressed(keyCode);
                return;

            case KEY_REPEATED:
                canvas.keyRepeated(keyCode);
                return;

            case KEY_RELEASED:
                canvas.keyReleased(keyCode);
                return;

            case POINTER_PRESSED:
                canvas.pointerPressed(pointer, x, y);
                return;

            case POINTER_DRAGGED:
                canvas.pointerDragged(pointer, x, y);
                return;

            case POINTER_RELEASED:
                canvas.pointerReleased(pointer, x, y);
                return;

            case SHOW_NOTIFY:
                canvas.showNotify();
                return;

            case HIDE_NOTIFY:
                canvas.hideNotify();
                return;

            case SIZE_CHANGED:
                canvas.sizeChanged(width, height);
                return;
        }
    }

    public void recycle() {
        canvas = null;
        recycled.push(this);
    }

    public void enterQueue() {
        enqueued[eventType]++;
    }

    public void leaveQueue() {
        enqueued[eventType]--;
    }

    public boolean placeableAfter(Event event) {
        if (event instanceof CanvasEvent) {
            switch (eventType) {
                case KEY_REPEATED:
                case POINTER_DRAGGED:
                    return enqueued[eventType] < 2;
                // return ((CanvasEvent)event).eventType != eventType;
            }
        }

        return true;
    }

//	public String toString()
//	{
//		switch(eventType)
//		{
//			case KEY_PRESSED:
//				return "keyPressed(" + keyCode + ")";
//				
//			case KEY_REPEATED:
//				return "keyRepeated(" + keyCode + ")";
//				
//			case KEY_RELEASED:
//				return "keyReleased(" + keyCode + ")";
//				
//			case POINTER_PRESSED:
//				return "pointerPressed(" + pointer + ", " + x + ", " + y + ")";
//				
//			case POINTER_DRAGGED:
//				return "pointerDragged(" + pointer + ", " + x + ", " + y + ")";
//				
//			case POINTER_RELEASED:
//				return "pointerReleased(" + pointer + ", " + x + ", " + y + ")";
//				
//			case SHOW_NOTIFY:
//				return "showNotify()";
//				
//			case HIDE_NOTIFY:
//				return "hideNotify()";
//				
//			case SIZE_CHANGED:
//				return "sizeChanged(" + width + ", " + height + ")";
//		}
//		
//		return "CanvasEvent(" + eventType + ")";
//	}
}
