package ua.naiksoftware.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

	private static final int BUFFER_SIZE = 2048;
	private static String sourceFolder;

	public static boolean zipFileAtPath(File sourceFile, File toLocation) {

		try {
			BufferedInputStream origin;
			FileOutputStream dest = new FileOutputStream(toLocation);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
					dest));
			sourceFolder = "/" + sourceFile.getName() + "/";
			if (sourceFile.isDirectory()) {
				zipSubFolder(out, sourceFile, sourceFile.getParent().length());
			} else {
				byte data[] = new byte[BUFFER_SIZE];
				FileInputStream fi = new FileInputStream(sourceFile);
				origin = new BufferedInputStream(fi, BUFFER_SIZE);
				ZipEntry entry = new ZipEntry(sourceFile.getName());
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
					out.write(data, 0, count);
				}
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static void zipSubFolder(ZipOutputStream out, File folder,
									 int basePathLength) throws IOException {
		File[] fileList = folder.listFiles();
		BufferedInputStream origin;
		for (File file : fileList) {
			if (file.isDirectory()) {
				zipSubFolder(out, file, basePathLength);
			} else {
				byte data[] = new byte[BUFFER_SIZE];
				String unmodifiedFilePath = file.getPath();
				String relativePath = unmodifiedFilePath
						.substring(basePathLength);
				relativePath = relativePath.replace(sourceFolder, "");
				FileInputStream fi = new FileInputStream(unmodifiedFilePath);
				origin = new BufferedInputStream(fi, BUFFER_SIZE);
				ZipEntry entry = new ZipEntry(relativePath);
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
			}
		}
	}

	public static boolean unzip(File zipFile, File extractFolder) {
		try {
			ZipFile zip = new ZipFile(zipFile);
			extractFolder.mkdir();
			Enumeration zipFileEntries = zip.entries();
			while (zipFileEntries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
				String currentEntry = entry.getName();
				File destFile = new File(extractFolder, currentEntry);
				File destinationParent = destFile.getParentFile();
				destinationParent.mkdirs();
				if (!entry.isDirectory()) {
					BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
					int currentByte;
					byte data[] = new byte[BUFFER_SIZE];
					// write the current file to disk
					FileOutputStream fos = new FileOutputStream(destFile);
					BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
					while ((currentByte = is.read(data, 0, BUFFER_SIZE)) != -1) {
						dest.write(data, 0, currentByte);
					}
					dest.flush();
					dest.close();
					is.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
