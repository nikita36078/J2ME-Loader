package ua.naiksoftware.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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

	public static boolean unzip(File zipFile, File folderToUnzip) throws FileNotFoundException {
		ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));
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
					byte[] bytes = new byte[BUFFER_SIZE];
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
}
