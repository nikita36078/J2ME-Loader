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

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.android.dx.command.dexer.Main;

import org.acra.ACRA;
import org.microemu.android.asm.AndroidProducer;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.applist.AppsListFragment;
import ru.playsoftware.j2meloader.config.Config;

public class JarConverter extends AsyncTask<String, String, Boolean> {

	public static final String TEMP_JAR_NAME = "tmp.jar";
	public static final String TEMP_URI_FOLDER_NAME = "tmp_uri";

	private static final String TEMP_FIX_FOLDER_NAME = "tmp_fix";
	private static final String TEMP_FOLDER_NAME = "tmp";
	private static final String TAG = JarConverter.class.getName();

	private final AppsListFragment fragment;
	private String err = "Void error";
	private ProgressDialog dialog;

	private String appDirPath;
	private String dataDirPath;
	private final File tmpDir;
	private File appConverted;

	public JarConverter(AppsListFragment fragment) {
		this.fragment = fragment;
		dataDirPath = fragment.getActivity().getApplicationInfo().dataDir;
		tmpDir = new File(dataDirPath, TEMP_FOLDER_NAME);
		tmpDir.mkdir();
	}

	@Override
	protected Boolean doInBackground(String... p1) {
		boolean jadInstall = false;
		String pathToJad = null;
		String pathToJar = p1[0];
		// Add jar name to ACRA
		String targetJarName = pathToJar.substring(pathToJar.lastIndexOf('/') + 1);
		ACRA.getErrorReporter().putCustomData("Last installed app", targetJarName);
		Log.d(TAG, "doInBackground$ pathToJar=" + pathToJar);
		// Check extension
		String extension = pathToJar.substring(pathToJar.lastIndexOf('.'), pathToJar.length());
		if (extension.equalsIgnoreCase(".jad")) {
			jadInstall = true;
			// Fix path to jar
			pathToJad = pathToJar;
			pathToJar = pathToJar.substring(0, pathToJar.length() - 1).concat("r");
		}
		File inputJar = new File(pathToJar);
		File fixedJar;
		try {
			fixedJar = fixJar(inputJar);
		} catch (Exception e) {
			e.printStackTrace();
			err = "Can't convert";
			deleteTemp();
			return false;
		}
		try {
			ZipUtils.unzip(fixedJar, tmpDir);
		} catch (IOException e) {
			e.printStackTrace();
			err = "Broken jar";
			deleteTemp();
			return false;
		}
		appDirPath = FileUtils.loadManifest(
				new File(tmpDir, "/META-INF/MANIFEST.MF")).get("MIDlet-Name");
		if (appDirPath == null) {
			err = "Brocken manifest";
			deleteTemp();
			return false;
		}
		// Remove invalid characters from app path
		appDirPath = appDirPath.replace(":", "").replace("/", "");
		appConverted = new File(Config.APP_DIR, appDirPath);
		FileUtils.deleteDirectory(appConverted);
		appConverted.mkdirs();
		Log.d(TAG, "appConverted=" + appConverted.getPath());
		try {
			Main.main(new String[]{
					"--no-optimize", "--output=" + appConverted.getPath()
					+ Config.MIDLET_DEX_FILE, fixedJar.getAbsolutePath()});
		} catch (IOException e) {
			e.printStackTrace();
			err = "Can't convert";
			deleteTemp();
			return false;
		}
		// Get midlet config file
		File conf;
		if (jadInstall) {
			conf = new File(pathToJad);
		} else {
			conf = new File(tmpDir, "/META-INF/MANIFEST.MF");
		}
		try {
			FileUtils.copyFileUsingChannel(conf, new File(appConverted, Config.MIDLET_MANIFEST_FILE));
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Extract other resources from jar.
		FileUtils.copyFiles(tmpDir.getPath(), Config.APP_DIR + appDirPath + Config.MIDLET_RES_DIR,
				(dir, fname) -> !(fname.endsWith(".class") || fname.endsWith(".jar.jar")));
		deleteTemp();
		return true;
	}

	@Override
	public void onPreExecute() {
		dialog = new ProgressDialog(fragment.getActivity());
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.setMessage(fragment.getText(R.string.converting_message));
		dialog.setTitle(R.string.converting_wait);
		dialog.show();
	}

	@Override
	public void onPostExecute(Boolean result) {
		Toast toast;
		if (result) {
			toast = Toast.makeText(fragment.getActivity(),
					fragment.getResources().getString(R.string.convert_complete) + " " + appDirPath, Toast.LENGTH_LONG);
			fragment.addApp(FileUtils.getApp(appConverted));
		} else {
			toast = Toast.makeText(fragment.getActivity(), err, Toast.LENGTH_LONG);
		}
		dialog.dismiss();
		toast.show();
	}

	private File fixJar(File inputJar) throws IOException {
		File fixedJar = new File(tmpDir, inputJar.getName() + ".jar");
		try {
			AndroidProducer.processJar(inputJar, fixedJar);
		} catch (ZipException e) {
			File unpackedJarFolder = new File(dataDirPath, TEMP_FIX_FOLDER_NAME);
			ZipUtils.unzip(inputJar, unpackedJarFolder);

			File repackedJar = new File(tmpDir, inputJar.getName());
			ZipUtils.zipFileAtPath(unpackedJarFolder, repackedJar);

			AndroidProducer.processJar(repackedJar, fixedJar);
			FileUtils.deleteDirectory(unpackedJarFolder);
			repackedJar.delete();
		}
		return fixedJar;
	}

	private void deleteTemp() {
		// Delete temp files
		FileUtils.deleteDirectory(tmpDir);
		File uriFolder = new File(dataDirPath, JarConverter.TEMP_URI_FOLDER_NAME);
		FileUtils.deleteDirectory(uriFolder);
	}
}
