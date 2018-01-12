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

package ua.naiksoftware.j2meloader;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.android.dx.command.Main;

import org.microemu.android.asm.AndroidProducer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipException;

import javax.microedition.shell.ConfigActivity;

import ua.naiksoftware.util.FileUtils;
import ua.naiksoftware.util.Log;
import ua.naiksoftware.util.ZipUtils;

public class JarConverter extends AsyncTask<String, String, Boolean> {

	public static final String TEMP_JAR_NAME = "tmp.jar";
	public static final String TEMP_URI_FOLDER_NAME = "tmp_uri";

	private static final String TEMP_FIX_FOLDER_NAME = "tmp_fix";
	private static final String TEMP_FOLDER_NAME = "tmp";
	private static final String TAG = "JarConverter";

	private final Context context;
	private String err = "Void error";
	private ProgressDialog dialog;

	private String appDir;
	private final File dirTmp;
	private static String targetJarName;

	public JarConverter(MainActivity context) {
		this.context = context;
		dirTmp = new File(context.getApplicationInfo().dataDir, TEMP_FOLDER_NAME);
		dirTmp.mkdir();
	}

	@Override
	protected Boolean doInBackground(String... p1) {
		String pathToJar = p1[0];
		targetJarName = pathToJar.substring(pathToJar.lastIndexOf('/') + 1);
		String pathConverted = p1[1];
		Log.d(TAG, "doInBackground$ pathToJar=" + pathToJar + " pathConverted="
				+ pathConverted);
		File inputJar = new File(pathToJar);
		File fixedJar;
		try {
			fixedJar = fixJar(inputJar);
		} catch (IOException e) {
			e.printStackTrace();
			err = "Can't convert";
			deleteTemp();
			return false;
		}
		try {
			ZipUtils.unzip(fixedJar, dirTmp);
		} catch (IOException e) {
			err = "Brocken jar";
			deleteTemp();
			return false;
		}
		appDir = FileUtils.loadManifest(
				new File(dirTmp, "/META-INF/MANIFEST.MF")).get("MIDlet-Name");
		if (appDir == null) {
			err = "Brocken manifest";
			deleteTemp();
			return false;
		}
		appDir = appDir.replace(":", "").replace("/", "");
		File appConverted = new File(pathConverted, appDir);
		FileUtils.deleteDirectory(appConverted);
		appConverted.mkdirs();
		Log.d(TAG, "appConverted=" + appConverted.getPath());
		Main.main(new String[]{
				"--dex", "--no-optimize", "--output=" + appConverted.getPath()
				+ ConfigActivity.MIDLET_DEX_FILE, fixedJar.getAbsolutePath()});
		File conf = new File(dirTmp, "/META-INF/MANIFEST.MF");
		try {
			FileUtils.copyFileUsingChannel(conf, new File(appConverted, ConfigActivity.MIDLET_CONF_FILE));
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Extract other resources from jar.
		FileUtils.moveFiles(dirTmp.getPath(), pathConverted + appDir
				+ ConfigActivity.MIDLET_RES_DIR, new FilenameFilter() {
			public boolean accept(File dir, String fname) {
				return !(fname.endsWith(".class") || fname.endsWith(".jar.jar"));
			}
		});
		deleteTemp();
		return true;
	}

	@Override
	public void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.setMessage(context.getText(R.string.converting_message));
		dialog.setTitle(R.string.converting_wait);
		dialog.show();
	}

	@Override
	public void onPostExecute(Boolean result) {
		Toast toast;
		if (result) {
			toast = Toast.makeText(context, context.getResources().getString(R.string.convert_complete) + " " + appDir, Toast.LENGTH_LONG);
			((MainActivity) context).updateApps();
		} else {
			toast = Toast.makeText(context, err, Toast.LENGTH_LONG);
		}
		dialog.dismiss();
		toast.show();
	}

	private File fixJar(File inputJar) throws IOException {
		File fixedJar = new File(dirTmp, inputJar.getName() + ".jar");
		try {
			AndroidProducer.processJar(inputJar, fixedJar, true);
		} catch (ZipException e) {
			File unpackedJarFolder = new File(context.getApplicationInfo().dataDir, TEMP_FIX_FOLDER_NAME);
			ZipUtils.unzip(inputJar, unpackedJarFolder);

			File repackedJar = new File(dirTmp, inputJar.getName());
			ZipUtils.zipFileAtPath(unpackedJarFolder, repackedJar);

			AndroidProducer.processJar(repackedJar, fixedJar, true);
			FileUtils.deleteDirectory(unpackedJarFolder);
			repackedJar.delete();
		}
		return fixedJar;
	}

	private void deleteTemp() {
		// Delete temp files
		FileUtils.deleteDirectory(dirTmp);
		File uriDir = new File(context.getApplicationInfo().dataDir, TEMP_URI_FOLDER_NAME);
		if (uriDir.exists()) {
			FileUtils.deleteDirectory(uriDir);
		}
	}

	public static String getTargetJarName() {
		return targetJarName;
	}
}
