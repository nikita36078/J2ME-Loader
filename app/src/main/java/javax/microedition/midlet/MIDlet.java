/*
 * Copyright 2012 Kulikov Dmitriy, Naik
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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.LinkedHashMap;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.util.ContextHolder;

import ua.naiksoftware.util.FileUtils;

public class MIDlet {

	private static Context context;
	private static LinkedHashMap<String, String> properties;

	private static boolean pauseAppCalled = false;
	private static boolean destroyAppCalled = false;

	public void start() {
		FileUtils.deleteDirectory(ContextHolder.getCacheDir());
		startApp();
	}

	public static void setMidletContext(Context c) {
		context = c;
		ContextHolder.setContext(context);
	}

	public static Context getMidletContext() {
		return context;
	}

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
		System.exit(1);
	}

	/**
	 * Вызывается в самом начале запуска оболочки, когда создается объект
	 * Application.
	 * <p>
	 * В частности, этот метод вызывается перед созданием Activity.
	 * Соответственно, если конфигурирование оболочки происходит через
	 * ConfigActivity, то в момент вызова этого метода состояние оболочки еще не
	 * определено: виртуальный экран имеет нулевой размер, размеры шрифтов не
	 * скорректированы, ...
	 */
	public void createApp() {
	}

	/**
	 * Вызывается при передаче управления мидлету.
	 * <p>
	 * Этот метод следует использовать вместо конструктора класса мидлета для
	 * его инициализации.
	 * <p>
	 * Если конфигурирование оболочки происходит через ConfigActivity, то в
	 * момент вызова этого метода состояние оболочки полностью определено:
	 * виртуальный экран имеет указанный пользователем размер, размеры шрифтов
	 * скорректированы в соответствии с разрешением экрана, ...
	 */
	public/* abstract */void initApp() {
	}

	/**
	 * Вызывается каждый раз, когда мидлет становится активным: при запуске
	 * после initApp(), при восстановлении из свернутого состояния, ...
	 */
	public/* abstract */void startApp() {
	}

	/**
	 * Вызывается каждый раз, когда мидлет становится на паузу: при сворачивании
	 * в фоновый режим, ...
	 */
	public/* abstract */void pauseApp() {
	}

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
	public static/* abstract */void destroyApp(boolean unconditional) {
	}

	/**
	 * Корректно вызвать destroyApp(). Во время выполнения этого метода вызовы
	 * notifyDestroyed() игнорируются.
	 *
	 * @param unconditional флаг безусловного завершения, для Android не имеет особого
	 *                      смысла
	 */
	public static final void callDestroyApp(boolean unconditional) {
		destroyAppCalled = true;
		destroyApp(unconditional);
		destroyAppCalled = false;
	}

	public void startActivity(Class cls) {
		Intent i = new Intent(context, cls);
		context.startActivity(i);
	}

	public boolean platformRequest(String url)
			throws ConnectionNotFoundException {
		try {
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
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
