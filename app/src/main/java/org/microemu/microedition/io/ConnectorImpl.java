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
package org.microemu.microedition.io;

import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;

/**
 * @author vlads Original MicroEmulator implementation of
 *         javax.microedition.Connector
 *         <p>
 *         TODO integrate with ImplementationInitialization
 */
public class ConnectorImpl extends ConnectorAdapter {

	/* The context to be used when loading classes */
	private AccessControlContext acc;

	private final String TAG = "ConnectorImpl";

	// TODO make this configurable
	public static boolean debugConnectionInvocations = false;

	private final boolean needPrivilegedCalls = isWebstart();

	public ConnectorImpl() {
		acc = AccessController.getContext();
	}

	private static boolean isWebstart() {
		try {
			return (System.getProperty("javawebstart.version") != null);
		} catch (SecurityException e) {
			// This is the case for Applet.
			return false;
		}
	}

	public Connection open(final String name, final int mode, final boolean timeouts) throws IOException {
		try {
			return (Connection) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws IOException {
					if (debugConnectionInvocations || needPrivilegedCalls) {
						return openSecureProxy(name, mode, timeouts, needPrivilegedCalls);
					} else {
						return openSecure(name, mode, timeouts);
					}
				}
			}, acc);
		} catch (PrivilegedActionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			}
			throw new IOException(e.toString());
		}
	}

	private static Class[] getAllInterfaces(Class klass) {
		Vector allInterfaces = new Vector();
		Class parent = klass;
		while (parent != null) {
			Class[] interfaces = parent.getInterfaces();
			Collections.addAll(allInterfaces, interfaces);
			parent = parent.getSuperclass();
		}

		return (Class[]) allInterfaces.toArray(new Class[allInterfaces.size()]);
	}

	private Connection openSecureProxy(String name, int mode, boolean timeouts, boolean needPrivilegedCalls)
			throws IOException {
		Connection origConnection = openSecure(name, mode, timeouts);
		Class connectionClass = null;
		Class[] interfaces = getAllInterfaces(origConnection.getClass());
		for (int i = 0; i < interfaces.length; i++) {
			if (Connection.class.isAssignableFrom(interfaces[i])) {
				connectionClass = interfaces[i];
				break;
			} else if (interfaces[i].getClass().getName().equals(Connection.class.getName())) {
				Log.e(TAG, "Connection interface loaded by different ClassLoader");
			}
		}
		if (connectionClass == null) {
			throw new ClassCastException(origConnection.getClass().getName() + " Connection expected");
		}
		return (Connection) Proxy.newProxyInstance(ConnectorImpl.class.getClassLoader(), interfaces,
				new ConnectionInvocationHandler(origConnection, needPrivilegedCalls));
	}

	private Connection openSecure(String name, int mode, boolean timeouts) throws IOException {
		String className = null;
		String protocol = null;
		try {
			try {
				protocol = name.substring(0, name.indexOf(':'));
				className = "org.microemu.cldc." + protocol + ".Connection";
				Class cl = Class.forName(className);
				Object inst = cl.newInstance();
				if (inst instanceof ConnectionImplementation) {
					return ((ConnectionImplementation) inst).openConnection(name, mode, timeouts);
				} else {
					throw new ClassNotFoundException();
				}
			} catch (ClassNotFoundException e) {
				Log.d(TAG, "connection [" + protocol + "] class not found", e);
				throw new ConnectionNotFoundException("connection [" + protocol + "] class not found");
			}
		} catch (InstantiationException e) {
			Log.e(TAG, "Unable to create" + className, e);
			throw new ConnectionNotFoundException();
		} catch (IllegalAccessException e) {
			Log.e(TAG, "Unable to create" + className, e);
			throw new ConnectionNotFoundException();
		}
	}
}
