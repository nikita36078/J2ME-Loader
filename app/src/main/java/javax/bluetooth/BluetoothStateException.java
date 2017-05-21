/**
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *
 *
 *  @version $Id$
 */ 

package javax.bluetooth;

import java.io.IOException;

/**
 * The <code>BluetoothStateException</code> is thrown when
 * a request is made to the Bluetooth system that
 * the system cannot support in its present state.  If, however, the
 * Bluetooth system was not in this state, it could support this operation.
 * For example, some Bluetooth systems do not allow the device to go into
 * inquiry mode if a connection is established.  This exception would be
 * thrown if <code>startInquiry()</code> were called.
 *
 * @version 1.0 February 11, 2002
 */
public class BluetoothStateException extends IOException {

	private static final long serialVersionUID = 1L;

	/**
     * Creates a new <code>BluetoothStateException</code> without a detail
     * message.
     */
	public BluetoothStateException() {
	}

    /**
     * Creates a <code>BluetoothStateException</code> with the specified
     * detail message.
     *
     * @param msg the reason for the exception
	 */

	public BluetoothStateException(String msg) {
		super(msg);
	}
}