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
package org.microemu.microedition;

import java.util.Map;

/**
 * @author vlads
 * 
 * Optional JSR implementation can be plugged to Emulator using this interfaces.
 * See module microemu-jsr-75 as example
 * 
 * Relevant MicroEmulator command line option
 * 
 * <pre>
 *  --impl JSR_implementation_class_name Initialize and register optional JSR implementation class.
 * </pre>
 * 
 */
public interface ImplementationInitialization {

	/**
	 * See "--id EmulatorID" command line option
	 */
	public static final String PARAM_EMULATOR_ID = "emulatorID";

	/**
	 * 
	 * Call implementation initialization inside secure context.
	 * 
	 * @param parameters
	 *            Map of configuration options and emulatorID property.
	 */
	public void registerImplementation(Map parameters);

	/**
	 * Called when MIDlet started
	 */
	public void notifyMIDletStart();

	/**
	 * Called when MIDlet exits or destroyed
	 */
	public void notifyMIDletDestroyed();
}
