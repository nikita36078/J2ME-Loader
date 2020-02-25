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

import android.util.Log;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.acra.ACRA;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.util.ContextHolder;

import androidx.annotation.Nullable;
import dalvik.system.DexClassLoader;
import ru.playsoftware.j2meloader.config.Config;

public class AppClassLoader extends DexClassLoader {
	private static final String TAG = ContextHolder.class.getName();

	private static File resFolder;
	private static ZipFile zipFile;

	public AppClassLoader(String paths, String tmpDir, ClassLoader parent, File resDir) {
		super(paths, tmpDir, null, new CoreClassLoader(parent));
		resFolder = resDir;
		prepareZipFile();
		ACRA.getErrorReporter().putCustomData("Running app", getName());
	}

	public static String getName() {
		return resFolder.getParentFile().getName();
	}

	@Nullable
	public static InputStream getResourceAsStream(Class resClass, String resName) {
		Log.d(TAG, "CUSTOM GET RES CALLED WITH PATH: " + resName);
		if (resName == null || resName.equals("")) {
			Log.w(TAG, "Can't load res on empty path");
			return null;
		}
		// Add support for Siemens file path
		String normName = resName.replace('\\', '/');
		// Remove double slashes
		normName = normName.replace("//", "/");
		if (normName.charAt(0) != '/' && resClass != null && resClass.getPackage() != null) {
			String className = resClass.getPackage().getName().replace('.', '/');
			normName = className + "/" + normName;
		}
		// Remove leading slash
		if (normName.charAt(0) == '/') {
			normName = normName.substring(1);
		}
		try {
			return getResourceStream(normName);
		} catch (IOException | NullPointerException e) {
			Log.w(TAG, "Can't load res: " + resName);
			return null;
		}
	}

	private static void prepareZipFile() {
		File midletResFile = new File(Config.APP_DIR,
				AppClassLoader.getName() + Config.MIDLET_RES_FILE);
		if (midletResFile.exists()) {
			zipFile = new ZipFile(midletResFile);
		}
	}

	private static InputStream getResourceStream(String resName) throws IOException {
		InputStream is;
		byte[] data;
		File midletResFile = new File(Config.APP_DIR,
				AppClassLoader.getName() + Config.MIDLET_RES_FILE);
		if (midletResFile.exists()) {
			FileHeader header = zipFile.getFileHeader(resName);
			is = zipFile.getInputStream(header);
			data = new byte[(int) header.getUncompressedSize()];
		} else {
			File resFile = new File(resFolder, resName);
			is = new FileInputStream(resFile);
			data = new byte[(int) resFile.length()];
		}
		DataInputStream dis = new DataInputStream(is);
		dis.readFully(data);
		dis.close();
		return new ByteArrayInputStream(data);
	}
}
