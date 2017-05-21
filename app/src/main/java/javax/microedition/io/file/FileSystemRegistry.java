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
package javax.microedition.io.file;

import java.util.Enumeration;

import org.microemu.cldc.file.FileSystemRegistryDelegate;
import org.microemu.microedition.ImplFactory;

public class FileSystemRegistry {

	private static FileSystemRegistryDelegate impl;

	static {
		impl = (FileSystemRegistryDelegate) ImplFactory.getImplementation(FileSystemRegistry.class, FileSystemRegistryDelegate.class);
	}

	private FileSystemRegistry() {
	}

	public static boolean addFileSystemListener(FileSystemListener listener) {
		return impl.addFileSystemListener(listener);
	}

	public static boolean removeFileSystemListener(FileSystemListener listener) {
		return impl.removeFileSystemListener(listener);
	}

	public static Enumeration listRoots() {
		return impl.listRoots();
	}

}
