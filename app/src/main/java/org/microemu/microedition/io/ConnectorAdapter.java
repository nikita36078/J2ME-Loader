/**
 *  MicroEmulator
 *  Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 *  @version $Id$
 */
package org.microemu.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

/**
 * @author vlads 
 * 
 * Default Connector
 */
public abstract class ConnectorAdapter implements ConnectorDelegate {

	public abstract Connection open(String name, int mode, boolean timeouts) throws IOException;

	public Connection open(String name) throws IOException {
		return open(name, Connector.READ_WRITE, false);
	}

	public Connection open(String name, int mode) throws IOException {
		return open(name, mode, false);
	}

	public DataInputStream openDataInputStream(String name) throws IOException {
		return ((InputConnection) open(name)).openDataInputStream();
	}

	public DataOutputStream openDataOutputStream(String name) throws IOException {
		return ((OutputConnection) open(name)).openDataOutputStream();
	}

	public InputStream openInputStream(String name) throws IOException {
		return ((InputConnection) open(name)).openInputStream();
	}

	public OutputStream openOutputStream(String name) throws IOException {
		return ((OutputConnection) open(name)).openOutputStream();
	}

}
