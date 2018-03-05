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

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;

/**
 * @author vlads Original MicroEmulator implementation of
 * javax.microedition.Connector
 * <p>
 * TODO integrate with ImplementationInitialization
 */
public class ConnectorImpl extends ConnectorAdapter {

	private final String TAG = ConnectorImpl.class.getName();

	@Override
	public Connection open(final String name, final int mode, final boolean timeouts) throws IOException {
		return openSecure(name, mode, timeouts);
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
