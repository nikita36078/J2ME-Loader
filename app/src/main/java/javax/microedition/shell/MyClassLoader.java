/*
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

package javax.microedition.shell;

import java.io.File;
import java.io.InputStream;

import javax.microedition.util.ContextHolder;

import dalvik.system.DexClassLoader;
import org.acra.ACRA;

public class MyClassLoader extends DexClassLoader {

	private static File resFolder;

	public MyClassLoader(String paths, String tmpDir, String libs, ClassLoader parent, String resDir) {
		super(paths, tmpDir, libs, parent);
		resFolder = new File(resDir);
		ACRA.getErrorReporter().putCustomData("Running app", getName());
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
