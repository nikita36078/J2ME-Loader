/*
 *  Nokia API for MicroEmulator
 *  Copyright (C) 2003 Markus Heberling <markus@heberling.net>
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
 *  Contributor(s):
 *    daniel(at)angrymachine.com.ar
 */

package com.nokia.mid.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;

public abstract class FullCanvas extends Canvas {
	public static final int KEY_SOFTKEY1 = -6;
	public static final int KEY_SOFTKEY2 = -7;
	public static final int KEY_SEND = -10;
	public static final int KEY_END = -11;
	public static final int KEY_SOFTKEY3 = -5;
	public static final int KEY_UP_ARROW = -1;
	public static final int KEY_DOWN_ARROW = -2;
	public static final int KEY_LEFT_ARROW = -3;
	public static final int KEY_RIGHT_ARROW = -4;

	/**
	 * Creates a new FullCanvas.
	 * Adds two empty commands to emulate softkey functions.
	 */
	protected FullCanvas() {
		super();
		super.setFullScreenMode(true);
	}

	/**
	 * Commands are not supported by FullCanvas
	 *
	 * @param cmd
	 */
	@Override
	public void addCommand(Command cmd) {
		throw new IllegalStateException();
	}

	/**
	 * Commands are not supported by FullCanvas
	 *
	 * @param l
	 */
	@Override
	public void setCommandListener(CommandListener l) {
		throw new IllegalStateException();
	}

}