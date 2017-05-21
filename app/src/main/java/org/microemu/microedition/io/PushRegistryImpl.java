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

import java.io.IOException;

import javax.microedition.io.ConnectionNotFoundException;

import org.microemu.microedition.Implementation;

/**
 * 
 * Default empty implemenation
 * 
 * @author vlads
 */

public class PushRegistryImpl implements PushRegistryDelegate, Implementation {

	public String getFilter(String connection) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getMIDlet(String connection) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] listConnections(boolean available) {
		// TODO Auto-generated method stub
		return new String[0];
	}

	public long registerAlarm(String midlet, long time) throws ClassNotFoundException, ConnectionNotFoundException {
		// TODO Auto-generated method stub
		throw new ConnectionNotFoundException();
	}

	public void registerConnection(String connection, String midlet, String filter) throws ClassNotFoundException,
			IOException {
		// TODO Auto-generated method stub

	}

	public boolean unregisterConnection(String connection) {
		// TODO Auto-generated method stub
		return false;
	}

}
