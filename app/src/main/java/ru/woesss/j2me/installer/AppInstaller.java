/*
 *  Copyright 2020 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.woesss.j2me.installer;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.dx.command.dexer.Main;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

import org.microemu.android.asm.AndroidProducer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

import io.reactivex.SingleEmitter;
import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.Constants;
import ru.playsoftware.j2meloader.util.ConverterException;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.playsoftware.j2meloader.util.ZipUtils;
import ru.woesss.j2me.jar.Descriptor;

public class AppInstaller {
	private static final String TAG = AppInstaller.class.getSimpleName();
	static final int STATUS_OLDEST = -1;
	static final int STATUS_EQUAL = 0;
	static final int STATUS_NEWEST = 1;
	static final int STATUS_NEW = 2;
	static final int STATUS_UNMATCHED = 3;

	private final Application context;
	private final AppRepository appRepository;
	private final Uri uri;
	private final File cacheDir;
	private Descriptor manifest;
	private Descriptor newDesc;
	private String appDirName;
	private File targetDir;
	private File srcJar;
	private File tmpDir;
	private AppItem currentApp;
	private File srcFile;

	AppInstaller(String path, Uri uri, Application context, AppRepository appRepository) {
		this.appRepository = appRepository;
		if (path != null) srcFile = new File(path);
		this.uri = uri;
		this.context = context;
		this.cacheDir = new File(context.getCacheDir(), "installer");
	}

	Descriptor getNewDescriptor() {
		return newDesc;
	}

	String getCurrentVersion() {
		return currentApp.getVersion();
	}

	Descriptor getManifest() {
		return manifest;
	}

	/** Load and check app info from source */
	void loadInfo(SingleEmitter<Integer> emitter) throws IOException, ConverterException {
		boolean isLocal;
		if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
			downloadJad();
			isLocal = false;
		} else {
			srcFile = FileUtils.getFileForUri(context, uri);
			isLocal = true;
		}

		String name = srcFile.getName();

		if (name.toLowerCase().endsWith(".jad")) {
			newDesc = new Descriptor(srcFile, true);
			String url = newDesc.getJarUrl();
			if (url == null) {
				throw new ConverterException("Jad not have " + Descriptor.MIDLET_JAR_URL);
			}
			Uri uri = Uri.parse(url);
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if (isLocal && scheme == null && host == null) {
				if (!checkJarFile(srcFile)) {
					emitter.onSuccess(STATUS_UNMATCHED);
					return;
				}
			}
		} else if (name.toLowerCase().endsWith(".kjx")) {
			/** Load kjx file */
			parseKjx();
			newDesc = new Descriptor(srcFile, true);
		} else {
			srcJar = srcFile;
			newDesc = loadManifest(srcFile);
		}
		int result = checkDescriptor();
		emitter.onSuccess(result);
	}

	private void parseKjx() throws ConverterException {
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new ConverterException("Can't create cache dir");
		}
		File kjxFile = srcFile;
		File jadFile = null;
		File jarFile = null;
		try (InputStream inputStream = new FileInputStream(kjxFile);
			 DataInputStream dis = new DataInputStream(inputStream);
		) {
			byte[] magic = new byte[3];
			dis.read(magic, 0, 3);
			if (!Arrays.equals(magic, "KJX".getBytes())) {
				throw new ConverterException("Magic KJX does not match: " + new String(magic));
			}

			byte startJadPos = dis.readByte();
			byte lenKjxFileName = dis.readByte();
			dis.skipBytes(lenKjxFileName);
			int lenJadFileContent = dis.readUnsignedShort();
			byte lenJadFileName = dis.readByte();
			byte[] jadFileName = new byte[lenJadFileName];
			dis.read(jadFileName, 0, lenJadFileName);
			String strJadFileName = new String(jadFileName);

			int bufSize = 2048;
			byte[] buf = new byte[bufSize];

			jadFile = new File(cacheDir, strJadFileName);
			try (FileOutputStream fos = new FileOutputStream(jadFile)) {
				int restSize = lenJadFileContent;
				while(restSize > 0) {
					int readSize = dis.read(buf, 0, Math.min(restSize, bufSize));
					fos.write(buf, 0, readSize);
					restSize -= readSize;
				}
			}

			jarFile = new File(cacheDir, strJadFileName.substring(0, strJadFileName.length() -4) + ".jar");
			try (FileOutputStream fos = new FileOutputStream(jarFile)) {
				int length = 0;
				while((length = dis.read(buf)) > 0) {
					fos.write(buf, 0, length);
				}
			}

			srcFile = jadFile;
			srcJar = jarFile;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void downloadJad() throws ConverterException {
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new ConverterException("Can't create cache dir");
		}
		srcFile = new File(cacheDir, "tmp.jad");
		String url = uri.toString();
		Log.d(TAG, "Downloading " + url);
		Exception exception;
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setInstanceFollowRedirects(true);
			connection.setReadTimeout(3 * 60 * 1000);
			connection.setConnectTimeout(15000);
			int code = connection.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_PERM
					|| code == HttpURLConnection.HTTP_MOVED_TEMP) {
				String urlStr = connection.getHeaderField("Location");
				connection.disconnect();
				connection = (HttpURLConnection) new URL(urlStr).openConnection();
				connection.setInstanceFollowRedirects(true);
				connection.setReadTimeout(3 * 60 * 1000);
				connection.setConnectTimeout(15000);
			}
			try (InputStream inputStream = connection.getInputStream();
				 OutputStream outputStream = new FileOutputStream(srcFile)) {
				byte[] buffer = new byte[2048];
				int length;
				while ((length = inputStream.read(buffer)) > 0) {
					outputStream.write(buffer, 0, length);
				}
			}
			connection.disconnect();
			Log.d(TAG, "Download complete");
			return;
		} catch (MalformedURLException e) {
			exception = e;
		} catch (FileNotFoundException e) {
			exception = e;
		} catch (IOException e) {
			exception = e;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		deleteTemp();
		throw new ConverterException("Can't download jad", exception);
	}

	/** Install app */
	void install(SingleEmitter<AppItem> emitter) throws ConverterException, IOException {
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new ConverterException("Can't create cache dir");
		}
		tmpDir = new File(targetDir.getParent(), ".tmp");
		if (!tmpDir.isDirectory() && !tmpDir.mkdirs())
			throw new ConverterException("Can't create directory: '" + targetDir + "'");
		if (srcJar == null) {
			srcJar = new File(cacheDir, "tmp.jar");
			downloadJar();
			manifest = loadManifest(srcJar);
			if (!manifest.equals(newDesc)) {
				throw new ConverterException("*Jad not matches with Jar");
			}
		}
		File patchedJar = new File(cacheDir, "patched.jar");
		AndroidProducer.processJar(srcJar, patchedJar);
		try {
			Main.main(new String[]{"--no-optimize",
					"--output=" + tmpDir + Config.MIDLET_DEX_FILE,
					patchedJar.getAbsolutePath()});
		} catch (Throwable e) {
			throw new ConverterException("Dexing error", e);
		}
		if (manifest != null) {
			manifest.merge(newDesc);
			newDesc = manifest;
		}
		File resJar = new File(tmpDir, Config.MIDLET_RES_FILE);
		FileUtils.copyFileUsingChannel(srcJar, resJar);
		String icon = newDesc.getIcon();
		File iconFile = new File(tmpDir, Config.MIDLET_ICON_FILE);
		if (icon != null) {
			try {
				ZipUtils.unzipEntry(resJar, icon, iconFile);
			} catch (IOException e) {
				Log.w(TAG, "Can't unzip icon: " + icon, e);
				icon = null;
				//noinspection ResultOfMethodCallIgnored
				iconFile.delete();
			}
		}
		newDesc.writeTo(new File(tmpDir, Config.MIDLET_MANIFEST_FILE));
		FileUtils.deleteDirectory(targetDir);
		if (!tmpDir.renameTo(targetDir)) {
			throw new ConverterException("Can't rename '" + tmpDir + "' to '" + targetDir + "'");
		}
		String name = newDesc.getName();
		String vendor = newDesc.getVendor();
		AppItem app = new AppItem(appDirName, name, vendor, newDesc.getVersion());
		if (icon != null) {
			app.setImagePathExt(Config.MIDLET_ICON_FILE);
		}
		if (currentApp != null) {
			app.setId(currentApp.getId());
			String path = currentApp.getPath();
			if (!path.equals(appDirName)) {
				File rms = new File(Config.getDataDir(), path);
				if (rms.exists()) {
					File newRms = new File(Config.getDataDir(), appDirName);
					FileUtils.deleteDirectory(newRms);
					rms.renameTo(newRms);
				}
				File config = new File(Config.getConfigsDir(), path);
				if (config.exists()) {
					File newConfig = new File(Config.getConfigsDir(), appDirName);
					FileUtils.deleteDirectory(newConfig);
					config.renameTo(newConfig);
				}
				File appDir = new File(Config.getAppDir(), path);
				FileUtils.deleteDirectory(appDir);
			}
		}
		emitter.onSuccess(app);
	}

	private Descriptor loadManifest(File jar) throws IOException {
		ZipFile zip = new ZipFile(jar);
		FileHeader manifest = zip.getFileHeader(JarFile.MANIFEST_NAME);
		if (manifest == null) throw new IOException("JAR not have " + JarFile.MANIFEST_NAME);
		try (ZipInputStream is = zip.getInputStream(manifest)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(20480);
			byte[] buf = new byte[4096];
			int read;
			while ((read = is.read(buf)) != -1) {
				baos.write(buf, 0, read);
			}
			return new Descriptor(baos.toString(), false);
		}
	}

	/** return true if JAR exists and matches JAD **/
	private boolean checkJarFile(File jad) throws IOException, ConverterException {
		File dir = jad.getParentFile();
		String jarUrl = newDesc.getJarUrl();
		File jar = new File(dir, jarUrl);
		if (!jar.exists()) {
			String name = jad.getName();
			jar = new File(dir, name.substring(0, name.length() - 4) + ".jar");
			if (!jar.exists()) {
				throw new ConverterException("Jar-file not found for url: " + jarUrl);
			}
		}
		srcJar = jar;
		manifest = loadManifest(jar);
		return manifest.equals(newDesc);
	}

	private int checkDescriptor() {
		// Remove invalid characters from app path
		String name = newDesc.getName();
		String vendor = newDesc.getVendor();
		currentApp = appRepository.get(name, vendor);
		String id = Integer.toHexString((name + vendor).hashCode());
		appDirName = name.replaceAll(FileUtils.ILLEGAL_FILENAME_CHARS, "").trim() + '_' + id;
		targetDir = new File(Config.getAppDir(), appDirName);
		if (currentApp == null) {
			return STATUS_NEW;
		}
		return newDesc.compareVersion(currentApp.getVersion());
	}

	private void downloadJar() throws ConverterException {
		Uri jarUri = Uri.parse(newDesc.getJarUrl());
		if (jarUri.getScheme() == null) {
			String schemeOfJadSource = this.uri.getScheme();
			if ("http".equals(schemeOfJadSource) || "https".equals(schemeOfJadSource)) {
				List<String> pathSegments = uri.getPathSegments();
				StringBuilder path = new StringBuilder(pathSegments.get(0));
				for (int i = 1; i < pathSegments.size() - 1; i++) {
					path.append('/').append(pathSegments.get(i));
				}
				path.append('/').append(jarUri.getPath());
				jarUri = uri.buildUpon().path(path.toString()).build();
			} else {
				jarUri = jarUri.buildUpon().scheme("http").build();
			}
		}
		String url = jarUri.toString();
		Log.d(TAG, "Downloading " + url);
		Exception exception;
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setInstanceFollowRedirects(true);
			connection.setReadTimeout(3 * 60 * 1000);
			connection.setConnectTimeout(15000);
			int code = connection.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_PERM
					|| code == HttpURLConnection.HTTP_MOVED_TEMP) {
				String urlStr = connection.getHeaderField("Location");
				connection.disconnect();
				connection = (HttpURLConnection) new URL(urlStr).openConnection();
				connection.setInstanceFollowRedirects(true);
				connection.setReadTimeout(3 * 60 * 1000);
				connection.setConnectTimeout(15000);
			}
			try (InputStream inputStream = connection.getInputStream();
				 OutputStream outputStream = new FileOutputStream(srcJar)) {
				byte[] buffer = new byte[2048];
				int length;
				while ((length = inputStream.read(buffer)) > 0) {
					outputStream.write(buffer, 0, length);
				}
			}
			connection.disconnect();
			Log.d(TAG, "Download complete");
			return;
		} catch (MalformedURLException e) {
			exception = e;
		} catch (FileNotFoundException e) {
			exception = e;
		} catch (IOException e) {
			exception = e;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		deleteTemp();
		throw new ConverterException("Can't download jar", exception);
	}

	void deleteTemp() {
		if (tmpDir != null) {
			FileUtils.deleteDirectory(tmpDir);
		}
	}

	public String getJar() {
		return srcJar == null ? null : srcJar.getAbsolutePath();
	}

	void clearCache() {
		FileUtils.deleteDirectory(cacheDir);
	}

	String getIconPath() {
		return targetDir.getAbsolutePath() + Config.MIDLET_ICON_FILE;
	}

	public AppItem getExistsApp() {
		return currentApp;
	}
}
