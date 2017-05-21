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
 * This {@code BluetoothConnectionException} is thrown when a Bluetooth 
 * connection (L2CAP, RFCOMM, or OBEX over RFCOMM) cannot be established
 * successfully. The fields in this exception class indicate the cause of
 * the exception. For example, an L2CAP connection may fail due to a 
 * security problem. This reason is passed on to the application through 
 * this class.
 *
 * @version 1.0 February 11, 2002
 *
 */
public class BluetoothConnectionException extends IOException {

	private static final long serialVersionUID = 1L;

	/**
	 * Indicates the connection to the server failed because no service 
	 * for the given PSM was registered.
	 * <p>
	 * The value for {@code UNKNOWN_PSM} is 0x0001 (1).
	 */
	public final static int UNKNOWN_PSM = 0x0001;
	
	/**
	 * Indicates the connection failed because the security settings on 
	 * the local device or the remote device were incompatible with the 
	 * request.
	 * <p>
	 * The value for {@code SECURITY_BLOCK} is 0x0002 (2).
	 */
	public final static int SECURITY_BLOCK = 0x002;
	/**
	 * Indicates the connection failed due to a lack of resources either 
	 * on the local device or on the remote device.
	 * <p>
	 * The value for {@code NO_RESOURCES} is 0x0003 (3).
	 */
	public final static int NO_RESOURCES = 0x0003;
	
	/**
	 * Indicates the connection to the server failed due to unknown 
	 * reasons.
	 * <p>
	 * The value for {@code FAILED_NOINFO} is 0x0004 (4).
	 */
	public final static int FAILED_NOINFO = 0x0004;

	/**
	 * Indicates the connection to the server failed due to a timeout.
	 * <p>
	 * The value for {@code TIMEOUT} is 0x0005 (5).
	 */
	public final static int TIMEOUT = 0x0005;
	
	/**
	 * Indicates the connection failed because the configuration 
	 * parameters provided were not acceptable to either the remote 
	 * device or the local device.
	 * <p>
	 * The value for {@code UNACCEPTABLE_PARAMS} is 0x0006 (6).
	 */
	public final static int UNACCEPTABLE_PARAMS = 0x0006;
	
	
	private	int	errorCode;
	
	/**
	 * Creates a new {@code BluetoothConnectionException} with the error 
	 * indicator specified.
	 * 
	 * @param error indicates the exception condition; must be one 
	 * 				of the constants described in this class
	 * @throws java.lang.IllegalArgumentException  if the input value 
	 * 					is not one of the constants in this class
	 */
	public BluetoothConnectionException(int error) {
		super();
		if(error < 1 || error > 6) {
			throw new java.lang.IllegalArgumentException();
		}
		errorCode = error;			
	}
	
	/**
	 * Creates a new {@code BluetoothConnectionException} with the error
	 * indicator and message specified.
     *
	 * @param error indicates the exception condition; must be one of 
	 * 				the constants described in this class
	 * @param msg a description of the exception; may by {@code null}
	 * @throws java.lang.IllegalArgumentException  if the input value 
	 * 					is not one of the constants in this class
	 */
	public BluetoothConnectionException(int error, String msg){
		super(msg);
		if (error < 1 || error > 6) {
			throw new java.lang.IllegalArgumentException();
		}
		errorCode = error;
	}
	/**
	 * Gets the status set in the constructor that will indicate the 
	 * reason for the exception.
	 * 
	 * @return cause for the exception; will be one of the constants 
	 * 			defined in this class
	 */
	public int getStatus() {
		return errorCode;
	}
}
