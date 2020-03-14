/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2018 Nikita Shakarun
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
package javax.microedition.midlet;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import java.util.Map;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Display;
import javax.microedition.util.ContextHolder;

public abstract class MIDlet {

	private static Map<String, String> properties;

	protected MIDlet() {
		Display.getDisplay(this); // init display for this instance
	}

	public static void initProps(Map<String, String> p) {
		properties = p;
	}

	public String getAppProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Report the shell that the MIDlet is ready to go into a pause.
	 */
	public final void notifyPaused() {
	}

	/**
	 * Report the shell that the MIDlet has completed its work. In this case, the shell will be
	 * closed.
	 * <p>
	 * Calls to this method from destroyApp() are ignored.
	 */
	public final void notifyDestroyed() {
		ContextHolder.notifyDestroyed();
	}

	/**
	 * Called every time the MIDlet becomes active: at startup
	 * after initApp(), when recovering from a minimized state.
	 */
	public abstract void startApp() throws MIDletStateChangeException;

	/**
	 * Called every time the MIDlet pauses: when minimized
	 * to background.
	 */
	public abstract void pauseApp();

	/**
	 * Called when the application terminates.
	 *
	 * @param unconditional unconditional completion flag, has no particular
	 *                      sense for Android.
	 */
	public abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

	public boolean platformRequest(String url)
			throws ConnectionNotFoundException {
		try {
			ContextHolder.getAppContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		} catch (ActivityNotFoundException e) {
			throw new ConnectionNotFoundException();
		}

		return true;
	}

	public final int checkPermission(String permission) {
		return 1;
	}

	public final void resumeRequest() {
	}
}
