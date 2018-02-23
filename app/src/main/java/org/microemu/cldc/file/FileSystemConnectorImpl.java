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

import android.util.Log;

import org.microemu.microedition.ImplementationUnloadable;
import org.microemu.microedition.io.ConnectorAdapter;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Vector;

import javax.microedition.io.Connection;

/**
 * @author vlads
 */
public class FileSystemConnectorImpl extends ConnectorAdapter implements ImplementationUnloadable {

	public final static String PROTOCOL = org.microemu.cldc.file.Connection.PROTOCOL;

	/* The context to be used when acessing filesystem */
	private AccessControlContext acc;

	private String fsRoot;

	private List openConnection = new Vector();

	FileSystemConnectorImpl(String fsRoot) {
		acc = AccessController.getContext();
		this.fsRoot = fsRoot;
	}

	public Connection open(final String name, int mode, boolean timeouts) throws IOException {
		// file://<host>/<path>
		if (!name.startsWith(PROTOCOL)) {
			throw new IOException("Invalid Protocol " + name);
		}

		Connection con = (Connection) doPrivilegedIO(() -> new FileSystemFileConnection(fsRoot, name.substring(PROTOCOL.length()),
				FileSystemConnectorImpl.this), acc);
		openConnection.add(con);
		return con;
	}

	static Object doPrivilegedIO(PrivilegedExceptionAction action, AccessControlContext context) throws IOException {
		try {
			return AccessController.doPrivileged(action, context);
		} catch (PrivilegedActionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			}
			throw new IOException(e.toString());
		}
	}

	void notifyMIDletDestroyed() {
		if (openConnection.size() > 0) {
			Log.w("FS", "Still has " + openConnection.size() + " open file connections");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.microemu.microedition.ImplementationUnloadable#unregisterImplementation()
	 */
	public void unregisterImplementation() {
		FileSystem.unregisterImplementation(this);
	}

	void notifyClosed(FileSystemFileConnection con) {
		openConnection.remove(con);
	}

}
