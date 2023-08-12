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
import android.util.Log;

import org.microemu.cldc.file.FileSystemFileConnection;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Display;
import javax.microedition.shell.MidletThread;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.util.AppUtils;

public abstract class MIDlet {
	private static final String TAG = MIDlet.class.getName();
	private static Map<String, String> properties;

	protected MIDlet() {
		Display.getDisplay(this); // init display for this instance
	}

	public static void initProps(Map<String, String> p) {
		properties = p;
	}

	public String getAppProperty(String key) {
		String value = properties.get(key);
		Log.d(TAG, "MIDlet.getAppProperty: " + key + "=" + value);
		return value;
	}

	/**
	 * Report the shell that the MIDlet is ready to go into a pause.
	 */
	public final void notifyPaused() {
		MidletThread.notifyPaused();
	}

	/**
	 * Report the shell that the MIDlet has completed its work. In this case, the shell will be
	 * closed.
	 * <p>
	 * Calls to this method from destroyApp() are ignored.
	 */
	public final void notifyDestroyed() {
		MidletThread.notifyDestroyed();
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

	public boolean platformRequest(String url) throws ConnectionNotFoundException {
		try {
			if (url.startsWith("localapp:")) {
				if (!url.contains("jam/launch?")) {
					throw new ConnectionNotFoundException("Protocol not supported");
				}
				parseJavaAppProtocol(url);
				return true;
			} else if (url.startsWith("javaapp:")) {
				parseJavaAppProtocol(url);
				return true;
			} else {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				if (url.startsWith("file://")) {
					FileSystemFileConnection fileConnection = (FileSystemFileConnection) Connector.open(url);
					intent.setData(fileConnection.getURI());
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					fileConnection.close();
				} else {
					intent.setData(Uri.parse(url));
				}
				ContextHolder.getActivity().startActivity(intent);
			}
		} catch (ConnectionNotFoundException e) {
			throw e;
		} catch (ActivityNotFoundException | IOException e) {
			throw new ConnectionNotFoundException(e);
		}
		return false;
	}

	private void parseJavaAppProtocol(String url) throws ConnectionNotFoundException {
		if (!url.contains("midlet-name") && !url.contains("midlet-uid")) {
			throw new ConnectionNotFoundException("No midlet-name value");
		}
		if (url.startsWith("localapp:")) {
			url = url.substring("localapp:".length());
		} else if(url.startsWith("javaapp:")) {
			url = url.substring("javaapp:".length());
		}
		if (url.startsWith("//")) {
			url = url.substring(2);
		}
		if (url.startsWith("jam/launch?")) {
			url = url.substring("jam/launch?".length());
		}
		url = URLDecoder.decode(url);
		String name = null;
		String vendor = null;
		String uid = null;
		String[] arr = url.split(";");

		StringBuilder argumentsBuilder = new StringBuilder();
		for (String s: arr) {
			if (s.length() == 0) {
				continue;
			}
			if (s.contains("=")) {
				int i = s.indexOf('=');
				String k = s.substring(0, i);
				String v = s.substring(i + 1);
				if (k.equals("midlet-name")) {
					name = v;
					continue;
				}
				if (k.equals("midlet-vendor")) {
					vendor = v;
					continue;
				}
				if (k.equals("midlet-uid")) {
					uid = v;
					continue;
				}
				if (k.equals("midlet-n")) {
					continue;
				}
				if (System.getProperty(k) == null) {
					argumentsBuilder.append(s).append(";");
				}
			} else {
				if (System.getProperty(s) == null) {
					argumentsBuilder.append(s).append(";");
				}
			}
		}
		if (name == null && uid == null) {
			throw new ConnectionNotFoundException();
		}
		argumentsBuilder.deleteCharAt(argumentsBuilder.length() - 1);
		final String arguments = argumentsBuilder.toString();
		try {
			final AppItem item = AppUtils.findApp(name, vendor, uid);
			if (item == null) {
				throw new ConnectionNotFoundException("App (" + name + ", " + vendor + ", " + uid + ") was not found!");
			}
			MidletThread.startAfterDestroy = new String[] { item.getTitle(), item.getPathExt(), arguments };
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConnectionNotFoundException(e);
		}
	}

	public final int checkPermission(String permission) {
		return 1;
	}

	public final void resumeRequest() {
		MidletThread.resumeApp();
	}
}
