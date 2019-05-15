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

package ru.playsoftware.j2meloader.util;

import android.util.Log;

import com.android.dx.command.dexer.Main;

import org.acra.ACRA;
import org.microemu.android.asm.AndroidProducer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;

import io.reactivex.Single;
import ru.playsoftware.j2meloader.config.Config;

public class JarConverter {

	public static final String TEMP_JAR_NAME = "tmp.jar";
	public static final String TEMP_JAD_NAME = "tmp.jad";
	public static final String TEMP_URI_FOLDER_NAME = "tmp_uri";

	private static final String TEMP_FOLDER_NAME = "tmp";
	private static final String TAG = JarConverter.class.getName();

	private String appDirPath;
	private String dataDirPath;
	private final File tmpDir;
	private File appConverted;

	public JarConverter(String dataDirPath) {
		this.dataDirPath = dataDirPath;
		tmpDir = new File(dataDirPath, TEMP_FOLDER_NAME);
	}

	private File patchJar(File inputJar, String encoding) throws IOException {
		File patchedJar = new File(tmpDir, inputJar.getName() + ".jar");
		AndroidProducer.processJar(inputJar, patchedJar, encoding);
		return patchedJar;
	}

	private void deleteTemp() {
		// Delete temp files
		FileUtils.deleteDirectory(tmpDir);
		File uriFolder = new File(dataDirPath, JarConverter.TEMP_URI_FOLDER_NAME);
		FileUtils.deleteDirectory(uriFolder);
	}

	private void download(String urlStr, File outputJar) throws IOException {
		// Download jar if it is referenced in jad file
		URL url = new URL(urlStr);
		Log.d(TAG, "Downloading " + outputJar.getPath());
		URLConnection connection = url.openConnection();
		connection.setReadTimeout(30000);
		connection.setConnectTimeout(15000);
		InputStream inputStream = connection.getInputStream();
		OutputStream outputStream = new FileOutputStream(outputJar);
		byte[] buffer = new byte[2048];
		int length;
		while ((length = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, length);
		}
		inputStream.close();
		outputStream.close();
		Log.d(TAG, "Download complete");
	}

	private File findManifest(File tmpDir) {
		String confName = "/META-INF/MANIFEST.MF";
		File conf = new File(tmpDir, confName);
		if (conf.exists()) {
			return conf;
		}
		// Manifest filename isn't in uppercase
		File parent = null;
		for (File file : tmpDir.listFiles()) {
			if (file.getName().equalsIgnoreCase(conf.getParentFile().getName())) {
				parent = file;
				break;
			}
		}
		if (parent == null) {
			return null;
		}
		for (File file : parent.listFiles()) {
			if (file.getName().equalsIgnoreCase(conf.getName())) {
				return file;
			}
		}
		return null;
	}

	public Single<String> convert(final String path, final String encoding) {
		return Single.create(emitter -> {
			boolean jadInstall = false;
			String pathToJad = null;
			String pathToJar = path;
			tmpDir.mkdir();

			// Add jar name to ACRA
			String targetJarName = pathToJar.substring(pathToJar.lastIndexOf('/') + 1);
			ACRA.getErrorReporter().putCustomData("Last installed app", targetJarName);
			Log.d(TAG, "doInBackground$ pathToJar=" + pathToJar);
			// Check extension
			String extension = pathToJar.substring(pathToJar.lastIndexOf('.'));
			if (extension.equalsIgnoreCase(".jad")) {
				jadInstall = true;
				// Fix path to jar
				pathToJad = pathToJar;
				pathToJar = pathToJar.substring(0, pathToJar.length() - 1).concat("r");
			}
			// Get jad file
			File conf = null;
			if (jadInstall) {
				conf = new File(pathToJad);
			}

			File inputJar = new File(pathToJar);
			// Check if jar exists
			if (jadInstall && !inputJar.exists()) {
				String url = FileUtils.loadManifest(conf).get("MIDlet-Jar-URL");
				try {
					download(url, inputJar);
				} catch (IOException e) {
					inputJar.delete();
					deleteTemp();
					throw new ConverterException("Can't download jar", e);
				}
			}
			// Patch and unzip
			File patchedJar;
			try {
				patchedJar = patchJar(inputJar, encoding);
			} catch (Exception e) {
				deleteTemp();
				throw new ConverterException("Can't patch", e);
			}
			try {
				ZipUtils.unzip(patchedJar, tmpDir);
			} catch (IOException e) {
				deleteTemp();
				throw new ConverterException("Invalid jar", e);
			}

			// Find manifest file and load it
			if (!jadInstall) {
				conf = findManifest(tmpDir);
				if (conf == null) {
					deleteTemp();
					throw new ConverterException("Manifest not found");
				}
			}
			LinkedHashMap<String, String> params = FileUtils.loadManifest(conf);
			appDirPath = params.get("MIDlet-Name");
			if (appDirPath == null) {
				deleteTemp();
				throw new ConverterException("Invalid manifest");
			}
			// Remove invalid characters from app path
			appDirPath = appDirPath.replace(":", "").replace("/", "");
			appConverted = new File(Config.APP_DIR, appDirPath);
			// Create target directory
			FileUtils.deleteDirectory(appConverted);
			appConverted.mkdirs();
			Log.d(TAG, "appConverted=" + appConverted.getPath());

			// Convert jar
			try {
				Main.main(new String[]{
						"--no-optimize", "--output=" + appConverted.getPath()
						+ Config.MIDLET_DEX_FILE, patchedJar.getAbsolutePath()});
			} catch (IOException e) {
				deleteTemp();
				FileUtils.deleteDirectory(appConverted);
				throw new ConverterException("Can't convert", e);
			}
			// Copy other resources from jar.
			try {
				FileUtils.copyFileUsingChannel(conf, new File(appConverted, Config.MIDLET_MANIFEST_FILE));
				File image = new File(tmpDir, AppUtils.getImagePathFromManifest(params));
				FileUtils.copyFileUsingChannel(image, new File(appConverted, Config.MIDLET_ICON_FILE));
			} catch (IOException | NullPointerException e) {
				e.printStackTrace();
			} catch (ArrayIndexOutOfBoundsException e) {
				deleteTemp();
				FileUtils.deleteDirectory(appConverted);
				throw new ConverterException("Invalid manifest");
			}
			FileUtils.copyFileUsingChannel(inputJar, new File(appConverted, Config.MIDLET_RES_FILE));
			deleteTemp();
			emitter.onSuccess(appDirPath);
		});
	}
}
