/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ua.naiksoftware.j2meloader;

import android.app.Application;

import com.devs.acr.AutoErrorReporter;

public class EmulatorApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		AutoErrorReporter.get(this)
				.setEmailAddresses("j2me.loader@mail.ru")
				.setEmailSubject("Auto Crash Report")
				.start();
	}
}
