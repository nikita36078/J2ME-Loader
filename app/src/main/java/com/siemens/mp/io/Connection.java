/*
 *  Siemens API for MicroEmulator
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
 */

package com.siemens.mp.io;

import com.siemens.mp.NotAllowedException;

public class Connection extends com.siemens.mp.misc.NativeMem {

	private final String connectTo;
	private static ConnectionListener sListener;
	private ConnectionListener mListener;

	public Connection(String connectTo) {
		this.connectTo = connectTo;
	}

	public ConnectionListener getListener() {
		return mListener;
	}

	public void send(byte[] data) throws NotAllowedException {
		if (sListener != null) {
			sListener.receiveData(data);
		}
		if (mListener != null) {
			mListener.receiveData(data);
		}
	}

	public void setListener(ConnectionListener listener) {
		mListener = listener;
	}

	// renamed for fix versioning problem
	public static void setListenerOld(ConnectionListener listener) {
		sListener = listener;
	}
}
