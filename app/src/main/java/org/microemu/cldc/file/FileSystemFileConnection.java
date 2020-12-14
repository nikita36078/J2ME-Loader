/*
 * MicroEmulator
 * Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2006-2007 Vlad Skarzhevskyy
 * <p>
 * It is licensed under the following two licenses as alternatives:
 * 1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 * 2. Apache License (the "AL") Version 2.0
 * <p>
 * You may not use this file except in compliance with at least one of
 * the above two licenses.
 * <p>
 * You may obtain a copy of the LGPL at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 * <p>
 * You may obtain a copy of the AL at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the LGPL or the AL for the specific language governing permissions and
 * limitations.
 *
 * @version $Id$
 */
package org.microemu.cldc.file;

import android.net.Uri;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.microedition.io.file.ConnectionClosedException;
import javax.microedition.io.file.FileConnection;

public class FileSystemFileConnection implements FileConnection {
	private static final String TAG = FileSystemFileConnection.class.getSimpleName();

	private static final char DIR_SEP = '/';
	private static final String DIR_SEP_STR = "/";
	private static final String[] FC_ROOTS = {
			"c:/",
			"e:/",
			"0:/",
			"1:/",
			"fs/MyStuff/",
	};
	private static final String[] FS_ROOTS = {System.getProperty("user.home")};

	private final String host;
	private final String root;
	private String name;
	private String path;
	private File file;

	private Throwable locationClosedFrom = null;
	private InputStream openedInputStream;
	private OutputStream openedOutputStream;

	FileSystemFileConnection(String url) throws IOException {
		// <host>/<path>
		Uri uri = Uri.parse(url);
		host = uri.getHost();
		if (host == null) {
			throw new IOException("Invalid connection specifier: " + url);
		}
		String path = uri.getPath();
		if (path == null || path.trim().length() == 0 || path.charAt(0) != DIR_SEP) {
			throw new IOException("Invalid connection specifier: " + url);
		}
		path = path.substring(1);
		root = getRoot(path);
		if (root == null) throw new IllegalArgumentException("Root is not specified: " + url);
		path = path.substring(root.length());
		String fsRootPath = getFsRoot();
		if (path.length() == 0) {
			file = new File(fsRootPath);
			this.path = "";
			name = "";
			return;
		}
		file = new File(fsRootPath, path);
		int nameSeparator = path.lastIndexOf(DIR_SEP, path.length() - 2);
		if (nameSeparator == -1) {
			name = path;
			this.path = "";
		} else {
			name = path.substring(nameSeparator + 1);
			this.path = path.substring(0, path.length() - name.length());
		}
		if (!name.endsWith(DIR_SEP_STR) && file.isDirectory()) {
			name += DIR_SEP;
		}
	}

	private String getFsRoot() {
		return FS_ROOTS[0] + DIR_SEP_STR;
	}

	private static String getRoot(String path) {
		for (String root : FC_ROOTS) {
			if (path.startsWith(root))
				return root;
		}
		int separator = path.indexOf(DIR_SEP);
		if (separator == -1) return null;
		Log.w(TAG, "getRoot: unknown root in path: " + path);
		return path.substring(0, separator + 1);
	}

	static Enumeration<String> listRoots() {
		Vector<String> list = new Vector<>();
		list.add(FC_ROOTS[0]);
		return list.elements();
	}

	@Override
	public long availableSize() {
		throwClosed();
		return file.getFreeSpace();
	}

	@Override
	public long totalSize() {
		throwClosed();
		return file.getTotalSpace();
	}

	@Override
	public boolean canRead() {
		throwClosed();
		return file.canRead();
	}

	@Override
	public boolean canWrite() {
		throwClosed();
		return file.canWrite();
	}

	@Override
	public void create() throws IOException {
		throwClosed();
		if (name.endsWith(DIR_SEP_STR)) {
			throw new IOException("This method can't create directories");
		}
		if (file.exists()) {
			throw new IOException("File already exists  " + file.getAbsolutePath());
		}
		if (!file.createNewFile()) {
			throw new IOException("Can't create file: " + file.getAbsolutePath());
		}
	}

	@Override
	public void delete() throws IOException {
		throwClosed();
		if (openedInputStream != null) {
			openedInputStream.close();
			openedInputStream = null;
		}
		if (openedOutputStream != null) {
			openedOutputStream.close();
			openedOutputStream = null;
		}
		if (!file.delete()) {
			throw new IOException("Unable to delete " + file.getAbsolutePath());
		}
	}

	@Override
	public long directorySize(final boolean includeSubDirs) throws IOException {
		throwClosed();
		if (!file.isDirectory()) {
			throw new IOException("Not a directory " + file.getAbsolutePath());
		}
		return directorySize(file, includeSubDirs);
	}

	private static long directorySize(File dir, boolean includeSubDirs) {
		long size = 0;

		File[] files = dir.listFiles();
		if (files == null) { // null if security restricted
			return 0L;
		}
		for (File child : files) {
			if (includeSubDirs && child.isDirectory()) {
				size += directorySize(child, true);
			} else {
				size += child.length();
			}
		}

		return size;
	}

	@Override
	public boolean exists() {
		throwClosed();
		return file.exists();
	}

	@Override
	public long fileSize() throws IOException {
		throwClosed();
		if (!file.isFile()) throw new IOException();
		return file.length();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		// returns Parent directory
		// /<root>/<directory>/
		return DIR_SEP + root + path;
	}

	@Override
	public String getURL() {
		// file://<host>/<root>/<directory>/<filename.extension>
		// or
		// file://<host>/<root>/<directory>/<directoryname>/
		try {
			URI uri = new URI("file", host, getPath() + getName(), null);
			return uri.toASCIIString();
		} catch (URISyntaxException e) {
			Log.e(TAG, "getURL: ", e);
			return Connection.PROTOCOL + this.host + DIR_SEP + getPath() + name;
		}
	}

	@Override
	public boolean isDirectory() {
		throwClosed();
		return file.isDirectory();
	}

	@Override
	public boolean isHidden() {
		throwClosed();
		return file.isHidden();
	}

	@Override
	public long lastModified() {
		throwClosed();
		return file.lastModified();
	}

	@Override
	public void mkdir() throws IOException {
		throwClosed();
		if (file.exists()) {
			throw new IOException("File exists");
		}
		if (!file.mkdir()) {
			throw new IOException("Can't create directory " + file.getAbsolutePath());
		}
		if (!name.endsWith(DIR_SEP_STR)) {
			name += DIR_SEP;
		}
	}

	@Override
	public Enumeration<String> list() throws IOException {
		return this.list(null, false);
	}

	@Override
	public Enumeration<String> list(final String filter, final boolean includeHidden) throws IOException {
		throwClosed();
		return listPrivileged(filter, includeHidden);
	}

	private Enumeration<String> listPrivileged(final String filter, boolean includeHidden) throws IOException {
		if (!this.file.isDirectory()) {
			throw new IOException("Not a directory " + this.file.getAbsolutePath());
		}
		FilenameFilter filenameFilter;
		if (filter == null) {
			filenameFilter = null;
		} else {
			filenameFilter = new FilenameFilter() {
				/* convert simple search pattern to regexp */
				private final Pattern pattern = Pattern.compile(filter.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*"));

				@Override
				public boolean accept(File dir, String name) {
					return pattern.matcher(name).matches();
				}
			};
		}

		File[] files = this.file.listFiles(filenameFilter);
		Vector<String> list = new Vector<>();
		if (files == null) {
			return list.elements();
		}
		Arrays.sort(files);
		for (File child : files) {
			if (!includeHidden && child.isHidden()) {
				continue;
			}
			String name = child.getName();
			if (child.isDirectory()) {
				name += DIR_SEP;
			}
			list.add(name);
		}
		return list.elements();
	}

	@Override
	public boolean isOpen() {
		return (this.file != null);
	}

	private void throwOpenDirectory() throws IOException {
		if (file.isDirectory()) {
			throw new IOException("Unable to open Stream on directory");
		}
	}

	@Override
	public InputStream openInputStream() throws IOException {
		throwClosed();
		throwOpenDirectory();

		if (this.openedInputStream != null) {
			throw new IOException("InputStream already opened");
		}
		// Trying to open more than one InputStream or more than one
		// OutputStream from a StreamConnection causes an IOException.
		this.openedInputStream = new FileInputStream(file) {
			@Override
			public void close() throws IOException {
				FileSystemFileConnection.this.openedInputStream = null;
				super.close();
			}
		};
		return this.openedInputStream;
	}

	@Override
	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		throwClosed();
		throwOpenDirectory();

		// Trying to open more than one InputStream or more than one
		// OutputStream from a StreamConnection causes an IOException.
		if (this.openedOutputStream != null) {
			throw new IOException("OutputStream already opened");
		}

		// TODO: 14.12.2020 unclear: should the existing file be truncated if the write hasn't reached the end
		this.openedOutputStream = new FileOutputStream(file, false) {
			@Override
			public void close() throws IOException {
				FileSystemFileConnection.this.openedOutputStream = null;
				super.close();
			}
		};
		return this.openedOutputStream;
	}

	@Override
	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	@Override
	public OutputStream openOutputStream(long byteOffset) throws IOException {
		throwClosed();
		throwOpenDirectory();

		// Trying to open more than one InputStream or more than one
		// OutputStream from a StreamConnection causes an IOException.
		if (this.openedOutputStream != null) {
			throw new IOException("OutputStream already opened");
		}
		// we cannot truncate the file here since it could already have content
		// which should be overridden instead of wiped.
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.seek(byteOffset);
		return new FileOutputStream(raf.getFD()) {
			@Override
			public void close() throws IOException {
				FileSystemFileConnection.this.openedOutputStream = null;
				super.close();
			}
		};
	}

	@Override
	public void rename(final String newName) throws IOException {
		throwClosed();
		if (newName.indexOf(DIR_SEP) != -1) {
			throw new IllegalArgumentException("Name contains path specification " + newName);
		}
		File newFile = new File(file.getParentFile(), newName);
		if (!file.renameTo(newFile)) {
			throw new IOException("Unable to rename " + file.getAbsolutePath() + " to "
					+ newFile.getAbsolutePath());
		}
		this.file = newFile;
		this.name = newName;
	}

	@Override
	public void setFileConnection(String fileName) throws IOException {
		throwClosed();
		if (fileName == null) {
			throw new NullPointerException();
		}
		if (!isDirectory()) {
			throw new IOException("Current FileConnection is not a directory");
		}
		File newFile;
		String newPath;
		String newName;
		if ("..".equals(fileName)) {
			newFile = file.getParentFile();
			if (newFile == null || (path.isEmpty() && name.isEmpty())) {
				throw new IOException("Cannot set FileConnection to '..' from a file system root");
			}
			int index = path.lastIndexOf(DIR_SEP, path.length() - 2);
			if (index == -1) {
				newPath = "";
				newName = path;
			} else {
				newPath = path.substring(0, index);
				newName = path.substring(index + 1);
			}
		} else {
			if (fileName.indexOf(DIR_SEP) != -1) {
				throw new IllegalArgumentException();
			}
			newFile = new File(file, fileName);
			newPath = path + name;
			newName = fileName;
		}
		if (!newFile.exists()) {
			throw new IllegalArgumentException();
		}
		file = newFile;
		path = newPath;
		name = newName;
	}

	@Override
	public void setHidden(boolean hidden) throws IOException {
		throwClosed();
	}

	@Override
	public void setReadable(boolean readable) throws IOException {
		throwClosed();
		file.setReadable(readable);
	}

	@Override
	public void setWritable(boolean writable) throws IOException {
		throwClosed();
		if (writable) {
			file.setWritable(true);
		} else {
			file.setReadOnly();
		}
	}

	@Override
	public void truncate(final long byteOffset) throws IOException {
		throwClosed();
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			raf.setLength(byteOffset);
		}
	}

	@Override
	public long usedSize() {
		try {
			return fileSize();
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public void close() throws IOException {
		if (this.file != null) {
			locationClosedFrom = new Throwable();
			locationClosedFrom.fillInStackTrace();
			this.file = null;
		}
	}

	private void throwClosed() throws ConnectionClosedException {
		if (this.file == null) {
			if (locationClosedFrom != null) {
				locationClosedFrom.printStackTrace();
			}
			throw new ConnectionClosedException("Connection already closed");
		}
	}
}
