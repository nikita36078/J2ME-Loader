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
package org.microemu.microedition;

import org.microemu.microedition.io.ConnectorDelegate;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 * This class allows to unbind implemenation with CLDC or MIDP declarations.
 *
 * @author vlads
 */
public class ImplFactory {

	public static final String DEFAULT = "org.microemu.default";

	private static final String INTERFACE_NAME_SUFIX = "Delegate";

	private static final String IMPLEMENTATION_NAME_SUFIX = "Impl";

	private Map implementations = new HashMap();

	private Map implementationsGCF = new HashMap();

	/* The context to be used when loading classes and resources */
	private AccessControlContext acc;

	/**
	 * Allow default initialization. In Secure environment instance() should be
	 * called initialy from secure contex.
	 */
	private static class SingletonHolder {
		private static ImplFactory instance = new ImplFactory();
	}

	private ImplFactory() {
		acc = AccessController.getContext();
	}

	public static ImplFactory instance() {
		return SingletonHolder.instance;
	}

	public static void register(Class delegate, Class implementationClass) {
		instance().implementations.put(delegate, implementationClass);
	}

	public static void register(Class delegate, Object implementationInstance) {
		instance().implementations.put(delegate, implementationInstance);
	}

	public static void unregister(Class delegate, Class implementation) {
		// TODO implement
	}

	/**
	 * Register Generic Connection Framework scheme implementation.
	 *
	 * @param implementation instance of ConnectorDelegate
	 * @param scheme
	 */
	public static void registerGCF(String scheme, Object implementation) {
		if (!ConnectorDelegate.class.isAssignableFrom(implementation.getClass())) {
			throw new IllegalArgumentException();
		}
		if (scheme == null) {
			scheme = DEFAULT;
		}
		Object impl = instance().implementationsGCF.get(scheme);
		if (impl instanceof ImplementationUnloadable) {
			((ImplementationUnloadable) impl).unregisterImplementation();
		}
		instance().implementationsGCF.put(scheme, implementation);
	}

	public static void unregistedGCF(String scheme, Object implementation) {
		if (!ConnectorDelegate.class.isAssignableFrom(implementation.getClass())) {
			throw new IllegalArgumentException();
		}
		if (scheme == null) {
			scheme = DEFAULT;
		}
		Object impl = instance().implementationsGCF.get(scheme);
		if (impl == implementation) {
			instance().implementationsGCF.remove(scheme);
		}
	}

	private Object getDefaultImplementation(Class delegateInterface) {
		try {
			String name = delegateInterface.getName();
			if (name.endsWith(INTERFACE_NAME_SUFIX)) {
				name = name.substring(0, name.length() - INTERFACE_NAME_SUFIX.length());
			}
			final String implClassName = name + IMPLEMENTATION_NAME_SUFIX;
			return AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
					Class implClass = ImplFactory.class.getClassLoader().loadClass(implClassName);
					try {
						implClass.getConstructor();
					} catch (NoSuchMethodException e) {
						throw new InstantiationException("No default constructor in class " + implClassName);
					}
					return implClass.newInstance();
				}
			}, acc);
		} catch (Throwable e) {
			throw new RuntimeException("Unable create " + delegateInterface.getName() + " implementation", e);
		}
	}

	private Object implementationNewInstance(final Class implClass) {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
					return implClass.newInstance();
				}
			}, acc);
		} catch (Throwable e) {
			throw new RuntimeException("Unable create " + implClass.getName() + " implementation", e);
		}
	}

	/**
	 * @param name The URL for the connection.
	 * @return UTL scheme
	 */
	public static String getCGFScheme(String name) {
		return name.substring(0, name.indexOf(':'));
	}

	/**
	 * @param name The URL for the connection.
	 * @return
	 */
	public static ConnectorDelegate getCGFImplementation(String name) {
		String scheme = getCGFScheme(name);
		ConnectorDelegate impl = (ConnectorDelegate) instance().implementationsGCF.get(scheme);
		if (impl != null) {
			return impl;
		}
		impl = (ConnectorDelegate) instance().implementationsGCF.get(DEFAULT);
		if (impl != null) {
			return impl;
		}
		return (ConnectorDelegate) instance().getDefaultImplementation(ConnectorDelegate.class);
	}

	// public static Implementation getImplementation(Class origClass, Object[]
	// constructorArgs) {
	// //TO-DO constructorArgs
	// return getImplementation(origClass);
	// }

	public static Implementation getImplementation(Class origClass, Class delegateInterface) {
		// if called from implementation constructor return null to avoid
		// recurive calls!
		// TODO can be done using thread stack analyse or ThreadLocal
		Object impl = instance().implementations.get(delegateInterface);
		// debugClassLoader(Implementation.class);
		// debugClassLoader(origClass);
		// debugClassLoader(delegateInterface);
		// debugClassLoader(o);

		if (impl != null) {
			if (impl instanceof Class) {
				return (Implementation) instance().implementationNewInstance((Class) impl);
			} else {
				return (Implementation) impl;
			}
		}
		return (Implementation) instance().getDefaultImplementation(delegateInterface);
	}
}
