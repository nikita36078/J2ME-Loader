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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.microedition.io.Connection;

/**
 * Dynamic proxy class for GCF Connections returend to MIDlet
 * Used to debug excetions thrown to MIDlet
 * Makes PrivilegedCalls when rinning in Webstart
 *
 * @author vlads
 */
public class ConnectionInvocationHandler implements InvocationHandler {

	private Connection originalConnection;
	private final String TAG = "ConnectionInvocation";

	/* The context to be used when connecting to network */
	private AccessControlContext acc;

	public ConnectionInvocationHandler(Connection con, boolean needPrivilegedCalls) {
		this.originalConnection = con;
		if (needPrivilegedCalls) {
			this.acc = AccessController.getContext();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		if (ConnectorImpl.debugConnectionInvocations) {
			Log.d(TAG, "invoke" + method.getName());
		}
		try {
			if (this.acc != null) {
				return AccessController.doPrivileged((PrivilegedExceptionAction) () -> method.invoke(originalConnection, args), acc);
			} else {
				return method.invoke(this.originalConnection, args);
			}
		} catch (PrivilegedActionException e) {
			if (e.getCause() instanceof InvocationTargetException) {
				if (ConnectorImpl.debugConnectionInvocations) {
					Log.e(TAG, "Connection." + method.getName(), e.getCause().getCause());
				}
				throw e.getCause().getCause();
			} else {
				if (ConnectorImpl.debugConnectionInvocations) {
					Log.e(TAG, "Connection." + method.getName(), e.getCause());
				}
				throw e.getCause();
			}
		} catch (InvocationTargetException e) {
			if (ConnectorImpl.debugConnectionInvocations) {
				Log.e(TAG, "Connection." + method.getName(), e.getCause());
			}
			throw e.getCause();
		}
	}

}
