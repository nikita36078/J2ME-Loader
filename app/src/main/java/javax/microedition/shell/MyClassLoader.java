/*
 * J2ME Loader
 * Copyright (C) 2015-2016 Nickolay Savchenko
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

package javax.microedition.shell;

import java.io.File;
import java.io.InputStream;

import javax.microedition.util.ContextHolder;

import dalvik.system.DexClassLoader;

public class MyClassLoader extends DexClassLoader {

	private static File resFolder;

	public MyClassLoader(String paths, String tmpDir, String libs, ClassLoader parent, String resDir) {
		super(paths, tmpDir, libs, parent);
		resFolder = new File(resDir);
	}

	@Override
	public InputStream getResourceAsStream(String resName) {
		return ContextHolder.getResourceAsStream(null, resName);
	}

	public static File getResFolder() {
		return resFolder;
	}

	public static String getName() {
		return resFolder.getParentFile().getName();
	}
}
