/**
 *  MicroEmulator
 *  Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net>
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
 */

package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.microemu.microedition.ImplFactory;

public class Connector {

	public static final int READ = 1;

	public static final int WRITE = 2;

	public static final int READ_WRITE = 3;

	private Connector() {
	    
	}
	
	public static Connection open(String name) throws IOException {
		return ImplFactory.getCGFImplementation(name).open(name);
	}

	public static Connection open(String name, int mode) throws IOException {
		return ImplFactory.getCGFImplementation(name).open(name, mode);
	}

	public static Connection open(String name, int mode, boolean timeouts) throws IOException {
		return ImplFactory.getCGFImplementation(name).open(name, mode, timeouts);
	}

	public static DataInputStream openDataInputStream(String name) throws IOException {
		return ImplFactory.getCGFImplementation(name).openDataInputStream(name);
	}

	public static DataOutputStream openDataOutputStream(String name) throws IOException {
		return ImplFactory.getCGFImplementation(name).openDataOutputStream(name);
	}

	public static InputStream openInputStream(String name) throws IOException {
		return ImplFactory.getCGFImplementation(name).openInputStream(name);
	}

	public static OutputStream openOutputStream(String name) throws IOException {
		return ImplFactory.getCGFImplementation(name).openOutputStream(name);
	}

}
