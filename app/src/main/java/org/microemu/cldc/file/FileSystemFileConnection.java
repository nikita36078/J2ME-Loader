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
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.microedition.io.file.ConnectionClosedException;
import javax.microedition.io.file.FileConnection;

public class FileSystemFileConnection implements FileConnection {

	private String fsRootConfig;

	private File fsRoot;

	private String host;

	private String fullPath;

	private File file;

	private boolean isRoot;

	private boolean isDirectory;

	private Throwable locationClosedFrom = null;

	private FileSystemConnectorImpl notifyClosed;

	private InputStream opendInputStream;

	private OutputStream opendOutputStream;

	private final static char DIR_SEP = '/';

	private final static String DIR_SEP_STR = "/";

	/* The context to be used when acessing filesystem */
	private AccessControlContext acc;

	private static boolean java15 = false;

	FileSystemFileConnection(String fsRootConfig, String name, FileSystemConnectorImpl notifyClosed) throws IOException {
		// <host>/<path>
		int hostEnd = name.indexOf(DIR_SEP);
		if (hostEnd == -1) {
			throw new IOException("Invalid path " + name);
		}
		this.fsRootConfig = fsRootConfig;
		this.notifyClosed = notifyClosed;

		host = name.substring(0, hostEnd);
		fullPath = name.substring(hostEnd + 1);
		if (fullPath.length() == 0) {
			throw new IOException("Invalid path " + name);
		}
		int rootEnd = fullPath.indexOf(DIR_SEP);
		isRoot = ((rootEnd == -1) || (rootEnd == fullPath.length() - 1));
		if (fullPath.charAt(fullPath.length() - 1) == DIR_SEP) {
			fullPath = fullPath.substring(0, fullPath.length() - 1);
		}
		acc = AccessController.getContext();
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				fsRoot = getRoot(FileSystemFileConnection.this.fsRootConfig);
				file = new File(fsRoot, fullPath);
				isDirectory = file.isDirectory();
				return null;
			}
		}, acc);
	}

	private Object doPrivilegedIO(PrivilegedExceptionAction action) throws IOException {
		return FileSystemConnectorImpl.doPrivilegedIO(action, acc);
	}

	private abstract class PrivilegedBooleanAction implements PrivilegedAction {
		public Object run() {
			return new Boolean(getBoolean());
		}

		abstract boolean getBoolean();
	}

	private boolean doPrivilegedBoolean(PrivilegedBooleanAction action) {
		return ((Boolean) AccessController.doPrivileged(action)).booleanValue();
	}

	public static File getRoot(String fsRootConfig) {
		try {
			File fsRoot = new File(System.getProperty("user.home"));
			if (!fsRoot.isDirectory()) {
				throw new RuntimeException("Can't find filesystem root " + fsRoot.getAbsolutePath());
			}
			return fsRoot;
		} catch (SecurityException e) {
			System.out.println("Cannot access user.home " + e);
			return null;
		}
	}

	static Enumeration listRoots(String fsRootConfig, String fsSingleConfig) {
		File[] files;
		if (fsSingleConfig != null) {
			files = new File[1];
			files[0] = getRoot(fsRootConfig + fsSingleConfig);
		} else {
			files = getRoot(fsRootConfig).listFiles();
			if (files == null) { // null if security restricted
				return (new Vector()).elements();
			}
		}
		Vector list = new Vector();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isHidden()) {
				continue;
			}
			if (file.isDirectory()) {
				list.add(file.getName() + DIR_SEP);
			}
		}
		return list.elements();
	}

	public long availableSize() {
		throwClosed();
		if (fsRoot == null) {
			return -1;
		}

		return getFileValueJava6("getFreeSpace");
	}

	public long totalSize() {
		throwClosed();
		if (fsRoot == null) {
			return -1;
		}
		return getFileValueJava6("getTotalSpace");
	}

	public boolean canRead() {
		throwClosed();
		return doPrivilegedBoolean(new PrivilegedBooleanAction() {
			public boolean getBoolean() {
				return file.canRead();
			}
		});
	}

	public boolean canWrite() {
		throwClosed();
		return doPrivilegedBoolean(new PrivilegedBooleanAction() {
			public boolean getBoolean() {
				return file.canWrite();
			}
		});
	}

	public void create() throws IOException {
		throwClosed();
		doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				if (!file.createNewFile()) {
					throw new IOException("File already exists  " + file.getAbsolutePath());
				}
				return null;
			}
		});
	}

	public void delete() throws IOException {
		throwClosed();
		doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				if (!file.delete()) {
					throw new IOException("Unable to delete " + file.getAbsolutePath());
				}
				return null;
			}
		});
	}

	public long directorySize(final boolean includeSubDirs) throws IOException {
		throwClosed();
		return ((Long) doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				if (!file.isDirectory()) {
					throw new IOException("Not a directory " + file.getAbsolutePath());
				}
				return new Long(directorySize(file, includeSubDirs));
			}
		})).longValue();
	}

	private static long directorySize(File dir, boolean includeSubDirs) throws IOException {
		long size = 0;

		File[] files = dir.listFiles();
		if (files == null) { // null if security restricted
			return 0L;
		}
		for (int i = 0; i < files.length; i++) {
			File child = files[i];

			if (includeSubDirs && child.isDirectory()) {
				size += directorySize(child, true);
			} else {
				size += child.length();
			}
		}

		return size;
	}

	public boolean exists() {
		throwClosed();
		return doPrivilegedBoolean(new PrivilegedBooleanAction() {
			public boolean getBoolean() {
				return file.exists();
			}
		});
	}

	public long fileSize() throws IOException {
		throwClosed();
		return ((Long) doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				return new Long(file.length());
			}
		})).longValue();
	}

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

	public String getURL() {
		// TODO test on real device. Not declared
		throwClosed();

		// file://<host>/<root>/<directory>/<filename.extension>
		// or
		// file://<host>/<root>/<directory>/<directoryname>/
		return Connection.PROTOCOL + this.host + DIR_SEP + fullPath + ((this.isDirectory) ? DIR_SEP_STR : "");
	}

	public boolean isDirectory() {
		throwClosed();
		return this.isDirectory;
	}

	public boolean isHidden() {
		throwClosed();
		return doPrivilegedBoolean(new PrivilegedBooleanAction() {
			public boolean getBoolean() {
				return file.isHidden();
			}
		});
	}

	public long lastModified() {
		throwClosed();
		return ((Long) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return new Long(file.lastModified());
			}
		}, acc)).longValue();
	}

	public void mkdir() throws IOException {
		throwClosed();
		doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				if (!file.mkdir()) {
					throw new IOException("Can't create directory " + file.getAbsolutePath());
				}
				return null;
			}
		});
	}

	public Enumeration list() throws IOException {
		return this.list(null, false);
	}

	public Enumeration list(final String filter, final boolean includeHidden) throws IOException {
		throwClosed();
		return (Enumeration) doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				return listPrivileged(filter, includeHidden);
			}
		});
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

				public boolean accept(File dir, String name) {
					return pattern.matcher(name).matches();
				}
			};
		}

		File[] files = this.file.listFiles(filenameFilter);
		if (files == null) { // null if security restricted
			return (new Vector()).elements();
		}
		Vector list = new Vector();
		for (int i = 0; i < files.length; i++) {
			File child = files[i];
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

	public boolean isOpen() {
		return (this.file != null);
	}

	private void throwOpenDirectory() throws IOException {
		if (this.isDirectory) {
			throw new IOException("Unable to open Stream on directory");
		}
	}

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
		this.opendInputStream = (InputStream) doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				return new FileInputStream(file) {
					public void close() throws IOException {
						FileSystemFileConnection.this.opendInputStream = null;
						super.close();
					}
				};
			}
		});
		return this.opendInputStream;
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

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
		this.opendOutputStream = (OutputStream) doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				return new FileOutputStream(file, append) {
					public void close() throws IOException {
						FileSystemFileConnection.this.opendOutputStream = null;
						super.close();
					}
				};
			}
		});
		return this.opendOutputStream;
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

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
		this.opendOutputStream = (OutputStream) doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				raf.seek(byteOffset);
				return new FileOutputStream(raf.getFD()) {
					public void close() throws IOException {
						FileSystemFileConnection.this.opendOutputStream = null;
						super.close();
					}
				};
			}
		});
		return this.opendOutputStream;
	}

	public void rename(final String newName) throws IOException {
		throwClosed();
		if (newName.indexOf(DIR_SEP) != -1) {
			throw new IllegalArgumentException("Name contains path specification " + newName);
		}
		doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				File newFile = new File(file.getParentFile(), newName);
				if (!file.renameTo(newFile)) {
					throw new IOException("Unable to rename " + file.getAbsolutePath() + " to "
							+ newFile.getAbsolutePath());
				}
				return null;
			}
		});
		this.fullPath = this.getPath() + newName;
	}

	public void setFileConnection(String s) throws IOException {
		throwClosed();
		// TODO Auto-generated method stub
	}

	public void setHidden(boolean hidden) throws IOException {
		throwClosed();
	}

	private void fileSetJava16(String mehtodName, final Boolean param) throws IOException {
		if (java15) {
			throw new IOException("Not supported on Java version < 6");
		}
		// Use Java6 function in reflection.
		try {
			final Method setWritable = file.getClass().getMethod(mehtodName, new Class[]{boolean.class});
			doPrivilegedIO(new PrivilegedExceptionAction() {
				public Object run() throws IOException {
					try {
						setWritable.invoke(file, new Object[]{param});
					} catch (Exception e) {
						throw new IOException(e.getCause().getMessage());
					}
					file.setReadOnly();
					return null;
				}
			});
		} catch (NoSuchMethodException e) {
			java15 = true;
			throw new IOException("Not supported on Java version < 6");
		}
	}

	private long getFileValueJava6(String mehtodName) throws SecurityException {
		if (java15) {
			throw new SecurityException("Not supported on Java version < 6");
		}
		// Use Java6 function in reflection.
		try {
			final Method getter = file.getClass().getMethod(mehtodName, new Class[]{});
			Long rc = (Long) doPrivilegedIO(new PrivilegedExceptionAction() {
				public Object run() throws IOException {
					try {
						return getter.invoke(file, new Object[]{});
					} catch (Exception e) {
						throw new IOException(e.getCause().getMessage());
					}
				}
			});
			return rc.longValue();
		} catch (IOException e) {
			throw new SecurityException(e.getMessage());
		} catch (NoSuchMethodException e) {
			java15 = true;
			throw new SecurityException("Not supported on Java version < 6");
		}
	}

	public void setReadable(boolean readable) throws IOException {
		throwClosed();
		fileSetJava16("setReadable", new Boolean(readable));
	}

	public void setWritable(boolean writable) throws IOException {
		throwClosed();
		if (!writable) {
			doPrivilegedIO(new PrivilegedExceptionAction() {
				public Object run() throws IOException {
					file.setReadOnly();
					return null;
				}
			});
		} else {
			fileSetJava16("setWritable", new Boolean(writable));
		}
	}

	public void truncate(final long byteOffset) throws IOException {
		throwClosed();
		doPrivilegedIO(new PrivilegedExceptionAction() {
			public Object run() throws IOException {
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				try {
					raf.setLength(byteOffset);
				} finally {
					raf.close();
				}
				return null;
			}
		});
	}

	public long usedSize() {
		try {
			return fileSize();
		} catch (IOException e) {
			return -1;
		}
	}

	public void close() throws IOException {
		if (this.file != null) {
			if (this.notifyClosed != null) {
				this.notifyClosed.notifyClosed(this);
			}
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
