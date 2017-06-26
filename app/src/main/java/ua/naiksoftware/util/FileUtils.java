package ua.naiksoftware.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Naik
 */
public class FileUtils {

	public static void moveFiles(String src, String dest, FilenameFilter filter) {
		File fsrc = new File(src);
		File fdest = new File(dest);
		fdest.mkdirs();
		String to;
		File[] list = fsrc.listFiles(filter);
		for (File entry : list) {
			to = entry.getPath().replace(src, dest);
			if (entry.isDirectory()) {
				moveFiles(entry.getPath(), to, filter);
			} else {
				entry.renameTo(new File(to));
			}
		}
	}

	public static void copyFileUsingChannel(File source, File dest) throws IOException {
		FileChannel sourceChannel = null;
		FileChannel destChannel = null;
		try {
			sourceChannel = new FileInputStream(source).getChannel();
			destChannel = new FileOutputStream(dest).getChannel();
			destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		} finally {
			sourceChannel.close();
			destChannel.close();
		}
	}

	public static boolean unzip(InputStream is, File folderToUnzip) {
		ZipInputStream zip = new ZipInputStream(new BufferedInputStream(is));
		ZipEntry zipEntry;
		try {
			while ((zipEntry = zip.getNextEntry()) != null) {
				String fileName = zipEntry.getName();
				final File outputFile = new File(folderToUnzip, fileName);
				outputFile.getParentFile().mkdirs();
				if (fileName.endsWith("/")) {
					outputFile.mkdirs();
					continue;
				} else {
					outputFile.createNewFile();
					FileOutputStream fos = new FileOutputStream(outputFile, false);
					byte[] bytes = new byte[2048];
					int c;
					try {
						while ((c = zip.read(bytes)) != -1) {
							fos.write(bytes, 0, c);
						}
						fos.flush();
						fos.close();
					} catch (IOException e) {
						Log.d("Unzip", "IOErr in readFromStream (zip.read(bytes)): " + e.getMessage());
						return false;
					}
				}
				zip.closeEntry();
			}
		} catch (IOException ioe) {
			Log.d("Unzip err", ioe.getMessage());
			return false;
		} finally {
			try {
				zip.close();
			} catch (Exception e) {
			}
		}
		return true;
	}

	public static void deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] listFiles = dir.listFiles();
			for (File file : listFiles) {
				deleteDirectory(file);
			}
		}
		dir.delete();
	}

	public static LinkedHashMap<String, String> loadManifest(File mf) {
		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mf)));
			String line;
			int index;
			while ((line = br.readLine()) != null) {
				index = line.indexOf(':');
				if (index > 0) {
					params.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
				}
				if (line.length() > 0 && Character.isWhitespace(line.charAt(0))) {
					Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator();
					Map.Entry<String, String> entry = null;
					while (iter.hasNext()) {
						entry = iter.next();
					}
					params.put(entry.getKey(), entry.getValue() + line.substring(1));
				}
			}
			br.close();
		} catch (Throwable t) {
			System.out.println("getAppProperty() will not be available due to " + t.toString());
		}
		return params;
	}
}
