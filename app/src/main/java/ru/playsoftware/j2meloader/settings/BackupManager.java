package ru.playsoftware.j2meloader.settings;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import ru.playsoftware.j2meloader.BuildConfig;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.FileUtils;

public class BackupManager {
	private static final String TAG = BackupManager.class.getSimpleName();
	private static final Gson GSON = new Gson();

	public static final String MANIFEST_ENTRY = "backup.json";
	public static final String FORMAT = "j2me-loader-backup";
	public static final int FORMAT_VERSION = 1;

	private static final int BUFFER_SIZE = 8192;
	private static final String TEMP_IMPORT_FILE = "backup-import.zip";

	private static final String[] SKIPPED_ROOT_FILES = {
			"J2ME-apps.db",
			"J2ME-apps.db-journal",
			"J2ME-apps.db-shm",
			"J2ME-apps.db-wal",
			".nomedia"
	};
	private static final String SKIPPED_APP_TEMP_DIR = "converted/.tmp";

	public interface ProgressListener {
		void onProgress(int percent);
	}

	private record BackupFile(File file, String path) {}

	private static class Manifest {
		@SerializedName("format")
		String format;
		@SerializedName("version")
		int version;
		@SerializedName("created")
		long created;
		@SerializedName("app_version")
		String appVersion;
	}

	public static void exportToZip(Context context, Uri uri, ProgressListener listener)
			throws IOException {
		File workDir = new File(Config.getEmulatorDir());
		if (!workDir.isDirectory()) {
			throw new IOException("Working directory not found: " + workDir);
		}
		List<BackupFile> files = new ArrayList<>();
		collectFiles(workDir, "", files);
		Collections.sort(files, (o1, o2) -> o1.path.compareTo(o2.path));

		long totalSize = 0;
		for (BackupFile file : files) {
			totalSize += file.file.length();
		}
		report(listener, 0);

		try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
			writeArchive(out, files, totalSize, listener);
		} catch (IOException | RuntimeException e) {
			Log.e(TAG, "exportToZip: ", e);
			throw e;
		}
		report(listener, 100);
	}

	public static void importFromZip(Context context, Uri uri, ProgressListener listener)
			throws IOException {
		File tmpFile = new File(context.getCacheDir(), TEMP_IMPORT_FILE);
		try {
			FileUtils.copyFileFromUri(context, uri, tmpFile);
			restoreArchive(tmpFile, listener);
			report(listener, 100);
		} catch (IOException | RuntimeException e) {
			Log.e(TAG, "importFromZip: ", e);
			throw e;
		} finally {
			//noinspection ResultOfMethodCallIgnored
			tmpFile.delete();
		}
	}

	private static void writeArchive(OutputStream target, List<BackupFile> files, long totalSize,
									 ProgressListener listener) throws IOException {
		try (ZipOutputStream out = new ZipOutputStream(
				new BufferedOutputStream(target, BUFFER_SIZE))) {
			writeManifest(out);

			byte[] buffer = new byte[BUFFER_SIZE];
			long written = 0;
			int lastPercent = 0;
			for (BackupFile backupFile : files) {
				ZipEntry entry = new ZipEntry(backupFile.path);
				out.putNextEntry(entry);
				try (InputStream in = new BufferedInputStream(
						new FileInputStream(backupFile.file), BUFFER_SIZE)) {
					int read;
					while ((read = in.read(buffer)) != -1) {
						out.write(buffer, 0, read);
						written += read;
					}
				}
				out.closeEntry();

				if (totalSize > 0) {
					int percent = (int) (written * 100 / totalSize);
					if (percent != lastPercent) {
						lastPercent = percent;
						report(listener, Math.min(percent, 99));
					}
				}
			}
		}
	}

	private static void writeManifest(ZipOutputStream out) throws IOException {
		Manifest manifest = new Manifest();
		manifest.format = FORMAT;
		manifest.version = FORMAT_VERSION;
		manifest.created = System.currentTimeMillis();
		manifest.appVersion = BuildConfig.VERSION_NAME;

		out.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
		out.write(GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
		out.closeEntry();
	}

	private static void restoreArchive(File archive, ProgressListener listener) throws IOException {
		File workDir = new File(Config.getEmulatorDir());
		if (!FileUtils.initWorkDir(workDir)) {
			throw new IOException("Can't create working directory: " + workDir);
		}
		String rootPath = workDir.getCanonicalPath();
		String rootPrefix = rootPath + File.separator;

		try (ZipFile zip = new ZipFile(archive)) {
			ZipEntry manifestEntry = zip.getEntry(MANIFEST_ENTRY);
			if (manifestEntry == null) {
				throw new IOException("Not a J2ME-Loader backup");
			}
			Manifest manifest;
			try (InputStream in = zip.getInputStream(manifestEntry);
				 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				manifest = GSON.fromJson(reader, Manifest.class);
			}
			if (manifest == null || !FORMAT.equals(manifest.format)) {
				throw new IOException("Not a J2ME-Loader backup");
			}
			if (manifest.version < 1 || manifest.version > FORMAT_VERSION) {
				throw new IOException("Unsupported backup version: " + manifest.version);
			}

			int count = zip.size();
			int index = 0;
			int lastPercent = -1;
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				index++;
				String name = normalizeEntryName(entry.getName());
				if (entry.isDirectory() || MANIFEST_ENTRY.equals(name) || isSkipped(name)) {
					continue;
				}
				if (!isSafeEntryName(name)) {
					throw new IOException("Invalid entry path: " + name);
				}
				File file = new File(workDir, name);
				String filePath = file.getCanonicalPath();
				if (!filePath.startsWith(rootPrefix)) {
					throw new IOException("Invalid entry path: " + name);
				}
				File parent = file.getParentFile();
				if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Can't create directory: " + parent);
				}
				try (InputStream in = new BufferedInputStream(zip.getInputStream(entry), BUFFER_SIZE);
					 OutputStream out = new BufferedOutputStream(
							 new FileOutputStream(file), BUFFER_SIZE)) {
					byte[] buffer = new byte[BUFFER_SIZE];
					int read;
					while ((read = in.read(buffer)) != -1) {
						out.write(buffer, 0, read);
					}
				}
				long modified = entry.getTime();
				if (modified > 0) {
					//noinspection ResultOfMethodCallIgnored
					file.setLastModified(modified);
				}

				if (count > 0) {
					int percent = index * 100 / count;
					if (percent != lastPercent) {
						lastPercent = percent;
						report(listener, Math.min(percent, 99));
					}
				}
			}
		}
	}

	private static void collectFiles(File root, String relativePath, List<BackupFile> out) {
		File dir = relativePath.isEmpty() ? root : new File(root, relativePath);
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			String path = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
			if (isSkipped(path)) {
				continue;
			}
			if (file.isDirectory()) {
				collectFiles(root, path, out);
			} else if (file.isFile()) {
				out.add(new BackupFile(file, path));
			}
		}
	}

	private static boolean isSkipped(String path) {
		if (path.equals(SKIPPED_APP_TEMP_DIR) || path.startsWith(SKIPPED_APP_TEMP_DIR + "/")) {
			return true;
		}
		if (path.indexOf('/') != -1) {
			return false;
		}
		for (String skipped : SKIPPED_ROOT_FILES) {
			if (skipped.equals(path)) {
				return true;
			}
		}
		return false;
	}

	private static String normalizeEntryName(String name) {
		String path = name.replace('\\', '/');
		while (path.startsWith("./")) {
			path = path.substring(2);
		}
		return path;
	}

	private static boolean isSafeEntryName(String path) {
		if (path.isEmpty() || path.startsWith("/")) {
			return false;
		}
		for (String segment : path.split("/")) {
			if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
				return false;
			}
		}
		return true;
	}

	private static void report(ProgressListener listener, int percent) {
		if (listener != null) {
			listener.onProgress(percent);
		}
	}
}