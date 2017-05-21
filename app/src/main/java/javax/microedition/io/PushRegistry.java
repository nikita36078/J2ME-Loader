/*
 *  MicroEmulator
 *  Copyright (C) 2001-2006 Bartek Teodorczyk <barteo@barteo.net>
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

package javax.microedition.io;

import java.io.IOException;

import org.microemu.microedition.ImplFactory;
import org.microemu.microedition.io.PushRegistryDelegate;

public class PushRegistry {

	private static PushRegistryDelegate impl;

	static {
		impl = (PushRegistryDelegate) ImplFactory.getImplementation(PushRegistry.class, PushRegistryDelegate.class);
	}

	public static void registerConnection(String connection, String midlet, String filter)
			throws ClassNotFoundException, IOException {
		impl.registerConnection(connection, midlet, filter);
	}

	public static boolean unregisterConnection(String connection) {
		return impl.unregisterConnection(connection);
	}

	public static String[] listConnections(boolean available) {
		return impl.listConnections(available);
	}

	public static String getMIDlet(String connection) {
		return impl.getMIDlet(connection);
	}

	public static String getFilter(String connection) {
		return impl.getFilter(connection);
	}

	public static long registerAlarm(String midlet, long time) throws ClassNotFoundException,
			ConnectionNotFoundException {
		return impl.registerAlarm(midlet, time);
	}

}
