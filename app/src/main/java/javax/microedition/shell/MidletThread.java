/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.microedition.shell;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.KeyMapper;
import javax.microedition.lcdui.event.CanvasEvent;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.util.ContextHolder;

import androidx.annotation.NonNull;

public class MidletThread extends HandlerThread implements Handler.Callback {
	private static final String TAG = MidletThread.class.getName();

	private static final int PAUSE = 2;
	private static final int START = 1;
	private static final int DESTROY = 3;
	private static MidletThread instance;
	private MIDlet midlet;
	private final Handler handler;
	private boolean started;

	private MidletThread(MicroLoader microLoader, String mainClass) {
		super("MidletMain");
		start();
		handler = new Handler(getLooper(), this);
		Runnable r = () -> {
			try {
				midlet = microLoader.loadMIDlet(mainClass);
				started = true;
				midlet.startApp();
			} catch (Throwable t) {
				t.printStackTrace();
				Throwable e;
				Throwable cause = t;
				while ((e = cause.getCause()) != null) {
					cause = e;
				}
				ContextHolder.getActivity().showErrorDialog(cause.toString());
			}
		};
		handler.post(r);
	}

	public static void create(MicroLoader microLoader, String mainClass) {
		instance = new MidletThread(microLoader, mainClass);
	}

	static void pauseApp() {
		if (instance != null)
			instance.handler.obtainMessage(PAUSE).sendToTarget();
	}

	static void resumeApp() {
		if (instance != null)
			instance.handler.obtainMessage(START).sendToTarget();
	}

	static void destroyApp() {
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Process.killProcess(Process.myPid());
		}, "ForceDestroyTimer").start();
		Displayable current = ContextHolder.getActivity().getCurrent();
		if (current instanceof Canvas) {
			Canvas canvas = (Canvas) current;
			int keyCode = KeyMapper.convertKeyCode(Canvas.KEY_END);
			Display.postEvent(CanvasEvent.getInstance(canvas, CanvasEvent.KEY_PRESSED, keyCode));
			Display.postEvent(CanvasEvent.getInstance(canvas, CanvasEvent.KEY_RELEASED, keyCode));
		}
		if (instance != null)
			instance.handler.obtainMessage(DESTROY, 1).sendToTarget();
	}

	@Override
	public boolean handleMessage(@NonNull Message msg) {
		if (midlet == null) return true;
		switch (msg.what) {
			case START:
				if (started) return true;
				started = true;
				try {
					midlet.startApp();
				} catch (MIDletStateChangeException e) {
					Log.w(TAG, "startApp:", e);
				} catch (Throwable t) {
					Log.e(TAG, "startApp:", t);
					ContextHolder.getActivity().showErrorDialog(t.getMessage());
				}
				break;
			case PAUSE:
				if (!started) return true;
				started = false;
				try {
					midlet.pauseApp();
				} catch (Throwable t) {
					Log.e(TAG, "pauseApp: ", t);
					ContextHolder.getActivity().showErrorDialog(t.getMessage());
				}
				break;
			case DESTROY:
				try {
					midlet.destroyApp(true);
					started = false;
					ContextHolder.notifyDestroyed();
				} catch (MIDletStateChangeException e) {
					Log.w(TAG, "destroyApp:", e);
					return true;
				} catch (Throwable t) {
					Log.e(TAG, "destroyApp:", t);
					ContextHolder.getActivity().showErrorDialog(t.getMessage());
				}
				break;
		}
		return true;
	}
}
