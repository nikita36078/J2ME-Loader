/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017 Nikita Shakarun
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

import java.util.LinkedHashMap;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.util.ContextHolder;

public abstract class MIDlet {

	private static LinkedHashMap<String, String> properties;
	private boolean pauseAppCalled = false;
	private boolean destroyAppCalled = false;

	public static void initProps(LinkedHashMap<String, String> p) {
		properties = p;
	}

	public String getAppProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Сообщить оболочке, что мидлет готов перейти в состояние паузы. При этом
	 * он будет свернут в фон.
	 * <p>
	 * Вызовы этого метода из pauseApp() игнорируются.
	 */
	public final void notifyPaused() {
		if (!pauseAppCalled) {
			ContextHolder.notifyPaused();
		}
	}

	/**
	 * Сообщить оболочке, что мидлет завершил работу. При этом оболочка будет
	 * закрыта.
	 * <p>
	 * Вызовы этого метода из destroyApp() игнорируются.
	 */
	public final void notifyDestroyed() {
		if (!destroyAppCalled) {
			ContextHolder.notifyDestroyed();
		}
	}

	/**
	 * Вызывается каждый раз, когда мидлет становится активным: при запуске
	 * после initApp(), при восстановлении из свернутого состояния, ...
	 */
	public abstract void startApp() throws MIDletStateChangeException;

	/**
	 * Вызывается каждый раз, когда мидлет становится на паузу: при сворачивании
	 * в фоновый режим, ...
	 */
	public abstract void pauseApp();

	/**
	 * Корректно вызвать pauseApp(). Во время выполнения этого метода вызовы
	 * notifyPaused() игнорируются.
	 */
	public final void callPauseApp() {
		pauseAppCalled = true;
		pauseApp();
		pauseAppCalled = false;
	}

	/**
	 * Вызывается при завершении работы приложения.
	 *
	 * @param unconditional флаг безусловного завершения, для Android не имеет особого
	 *                      смысла
	 */
	public abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

	/**
	 * Корректно вызвать destroyApp(). Во время выполнения этого метода вызовы
	 * notifyDestroyed() игнорируются.
	 *
	 * @param unconditional флаг безусловного завершения, для Android не имеет особого
	 *                      смысла
	 */
	public final void callDestroyApp(boolean unconditional) {
		destroyAppCalled = true;
		try {
			destroyApp(unconditional);
		} catch (MIDletStateChangeException e) {
			e.printStackTrace();
		}
		destroyAppCalled = false;
	}

	public boolean platformRequest(String url)
			throws ConnectionNotFoundException {
		try {
			ContextHolder.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		} catch (ActivityNotFoundException e) {
			throw new ConnectionNotFoundException();
		}

		return true;
	}

	public final int checkPermission(String str) {
		return -1;
	}

	public final void resumeRequest() {
	}
}
