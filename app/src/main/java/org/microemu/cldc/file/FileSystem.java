/**
 * MicroEmulator
 * Copyright (C) 2006-2007 Bartek Teodorczyk <barteo@barteo.net>
 * Copyright (C) 2006-2007 Vlad Skarzhevskyy
 * Copyright (C) 2017 Nikita Shakarun
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

import org.microemu.microedition.ImplFactory;
import org.microemu.microedition.ImplementationInitialization;

import java.util.Map;

/**
 * @author vlads
 *         <p>
 *         config2.xml example
 *         <p>
 *         <pre>
 *                  &lt;extensions&gt;
 *                  &lt;extension&gt;
 *                  &lt;className&gt;org.microemu.cldc.file.FileSystem&lt;/className&gt;
 *                  &lt;properties&gt;
 *                  &lt;property NAME=&quot;fsRoot&quot; VALUE=&quot;C:&quot;/&gt;
 *                  &lt;/properties&gt;
 *                  &lt;/extension&gt;
 *                  &lt;/extensions&gt;
 *                 </pre>
 */

public class FileSystem implements ImplementationInitialization {

	public static final String fsRootConfigProperty = "fsRoot";

	/**
	 * fsSingle defines explicitly single root inside the fsRoot, if fsSingle is null then
	 * default behavior is chosen
	 */
	public static final String fsSingleConfigProperty = "fsSingle";

	private FileSystemConnectorImpl impl;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.microemu.microedition.ImplementationInitialization#registerImplementation()
	 */
	public void registerImplementation(Map parameters) {
		String fsRoot = (String) parameters.get(fsRootConfigProperty);
		String fsSingle = (String) parameters.get(fsSingleConfigProperty);
		this.impl = new FileSystemConnectorImpl(fsRoot);
		ImplFactory.registerGCF("file", this.impl);
		ImplFactory.register(FileSystemRegistryDelegate.class, new FileSystemRegistryImpl(fsRoot, fsSingle));
	}

	protected static void unregisterImplementation(FileSystemConnectorImpl impl) {
		ImplFactory.unregistedGCF("file", impl);
		ImplFactory.unregister(FileSystemRegistryDelegate.class, FileSystemRegistryImpl.class);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.microemu.microedition.ImplementationInitialization#notifyMIDletStart()
	 */
	public void notifyMIDletStart() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.microemu.microedition.ImplementationInitialization#notifyMIDletDestroyed()
	 */
	public void notifyMIDletDestroyed() {
		this.impl.notifyMIDletDestroyed();
	}

}
