/**
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.microedition.io.file.ConnectionClosedException;
import javax.microedition.io.file.FileConnection;

public class FileSystemFileConnection implements FileConnection {

	private File fsRoot;

	private String host;

	private String fullPath;

	private File file;

	private boolean isRoot;

	private boolean isDirectory;

	private Throwable locationClosedFrom = null;

	private InputStream opendInputStream;

	private OutputStream opendOutputStream;

	private final static String ROOT_INTERNAL_DIR = "c:/";

	private final static String ROOT_INTERNAL_DIR_SIEMENS = "0:/";

	private final static String ROOT_INTERNAL_DIR_SIEMENS_2 = "fs/MyStuff/";

	private final static char DIR_SEP = '/';

	private final static String DIR_SEP_STR = "/";

	private static String TAG = FileSystemFileConnection.class.getName();

	FileSystemFileConnection(String name) throws IOException {
		// <host>/<path>
		int hostEnd = name.indexOf(DIR_SEP);
		if (hostEnd == -1) {
			throw new IOException("Invalid path " + name);
		}

		host = name.substring(0, hostEnd);
		fullPath = name.substring(hostEnd + 1);
		if (fullPath.length() == 0) {
			throw new IOException("Invalid path " + name);
		}
		int rootEnd = fullPath.indexOf(DIR_SEP);
		isRoot = ((rootEnd == -1) || (rootEnd == fullPath.length() - 1));
		fsRoot = getRoot(fullPath);
		file = new File(fsRoot, fullPath.substring(rootEnd));
		isDirectory = file.isDirectory();
	}


	private static File getRoot(String path) {
		File fsRoot = new File(System.getProperty("user.home"));
		return fsRoot;
	}

	static Enumeration listRoots() {
		Vector list = new Vector();
		list.add(ROOT_INTERNAL_DIR);
		return list.elements();
	}

	@Override
	public long availableSize() {
		throwClosed();
		if (fsRoot == null) {
			return -1;
		}

		return file.getFreeSpace();
	}

	@Override
	public long totalSize() {
		throwClosed();
		if (fsRoot == null) {
			return -1;
		}
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
		if (!file.createNewFile()) {
			throw new IOException("File already exists  " + file.getAbsolutePath());
		}
	}

	@Override
	public void delete() throws IOException {
		throwClosed();
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
		return new Long(directorySize(file, includeSubDirs));
	}

	private static long directorySize(File dir, boolean includeSubDirs) throws IOException {
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
		return file.length();
	}

	@Override
	public String getName() {
		// TODO test on real device. Not declared
		throwClosed();

		if (isRoot) {
			return "";
		}

		if (this.isDirectory) {
			return this.file.getName() + DIR_SEP;
		} else {
			return this.file.getName();
		}
	}

	@Override
	public String getPath() {
		// TODO test on real device. Not declared
		throwClosed();

		// returns Parent directory
		// /<root>/<directory>/
		if (isRoot) {
			return DIR_SEP + fullPath + DIR_SEP;
		}

		int pathEnd = fullPath.lastIndexOf(DIR_SEP);
		if (pathEnd == -1) {
			return DIR_SEP_STR;
		}
		return DIR_SEP + fullPath.substring(0, pathEnd + 1);
	}

	@Override
	public String getURL() {
		// TODO test on real device. Not declared
		throwClosed();

		// file://<host>/<root>/<directory>/<filename.extension>
		// or
		// file://<host>/<root>/<directory>/<directoryname>/
		return Connection.PROTOCOL + this.host + DIR_SEP + fullPath + ((this.isDirectory) ? DIR_SEP_STR : "");
	}

	@Override
	public boolean isDirectory() {
		throwClosed();
		return this.isDirectory;
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
		if (!file.mkdir()) {
			throw new IOException("Can't create directory " + file.getAbsolutePath());
		}
	}

	@Override
	public Enumeration list() throws IOException {
		return this.list(null, false);
	}

	@Override
	public Enumeration list(final String filter, final boolean includeHidden) throws IOException {
		throwClosed();
		return listPrivileged(filter, includeHidden);
	}

	private Enumeration listPrivileged(final String filter, boolean includeHidden) throws IOException {
		if (!this.file.isDirectory()) {
			throw new IOException("Not a directory " + this.file.getAbsolutePath());
		}
		FilenameFilter filenameFilter = null;
		if (filter != null) {
			filenameFilter = new FilenameFilter() {
				private Pattern pattern;

				{
					/* convert simple search pattern to regexp */
					pattern = Pattern.compile(filter.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*"));
				}

				@Override
				public boolean accept(File dir, String name) {
					return pattern.matcher(name).matches();
				}
			};
		}

		File[] files = this.file.listFiles(filenameFilter);
		if (files == null) { // null if security restricted
			return (new Vector()).elements();
		}
		Arrays.sort(files);
		Vector list = new Vector();
		for (File child : files) {
			if ((!includeHidden) && (child.isHidden())) {
				continue;
			}
			if (child.isDirectory()) {
				list.add(child.getName() + DIR_SEP);
			} else {
				list.add(child.getName());
			}
		}
		return list.elements();
	}

	@Override
	public boolean isOpen() {
		return (this.file != null);
	}

	private void throwOpenDirectory() throws IOException {
		if (this.isDirectory) {
			throw new IOException("Unable to open Stream on directory");
		}
	}

	@Override
	public InputStream openInputStream() throws IOException {
		throwClosed();
		throwOpenDirectory();

		if (this.opendInputStream != null) {
			throw new IOException("InputStream already opened");
		}
		/**
		 * Trying to open more than one InputStream or more than one
		 * OutputStream from a StreamConnection causes an IOException.
		 */
		this.opendInputStream = new FileInputStream(file) {
			@Override
			public void close() throws IOException {
				FileSystemFileConnection.this.opendInputStream = null;
				super.close();
			}
		};
		return this.opendInputStream;
	}

	@Override
	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		return openOutputStream(false);
	}

	private OutputStream openOutputStream(final boolean append) throws IOException {
		throwClosed();
		throwOpenDirectory();

		if (this.opendOutputStream != null) {
			throw new IOException("OutputStream already opened");
		}
		/**
		 * Trying to open more than one InputStream or more than one
		 * OutputStream from a StreamConnection causes an IOException.
		 */
		this.opendOutputStream = new FileOutputStream(file, append) {
			@Override
			public void close() throws IOException {
				FileSystemFileConnection.this.opendOutputStream = null;
				super.close();
			}
		};
		return this.opendOutputStream;
	}

	@Override
	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	@Override
	public OutputStream openOutputStream(long byteOffset) throws IOException {
		throwClosed();
		throwOpenDirectory();
		if (this.opendOutputStream != null) {
			throw new IOException("OutputStream already opened");
		}
		// we cannot truncate the file here since it could already have content
		// which should be overridden instead of wiped.

		return openOutputStream(true, byteOffset);
	}

	private OutputStream openOutputStream(boolean appendToFile, final long byteOffset) throws IOException {
		throwClosed();
		throwOpenDirectory();

		if (this.opendOutputStream != null) {
			throw new IOException("OutputStream already opened");
		}
		/**
		 * Trying to open more than one InputStream or more than one
		 * OutputStream from a StreamConnection causes an IOException.
		 */
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.seek(byteOffset);
		return new FileOutputStream(raf.getFD()) {
			@Override
			public void close() throws IOException {
				FileSystemFileConnection.this.opendOutputStream = null;
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
		this.fullPath = this.getPath() + newName;
	}

	@Override
	public void setFileConnection(String s) throws IOException {
		throwClosed();
		// TODO Auto-generated method stub
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
		if (!writable) {
			file.setReadOnly();
		} else {
			file.setWritable(writable);
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
