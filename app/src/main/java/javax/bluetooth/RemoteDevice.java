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

import javax.microedition.io.Connection;

/**
 * The <code>RemoteDevice</code> class represents a remote Bluetooth device.
 * It provides basic
 * information about a remote device including the device's Bluetooth address and
 * its friendly name.
 *
 * @version 1.0 February 11, 2002
 */
public class RemoteDevice {

	/**
	 * Creates a Bluetooth device based upon its address.  The Bluetooth
	 * address must be 12 hex characters long.  Valid characters are 0-9, a-f,
	 * and A-F.  There is no preceding "0x" in the string.  For example, valid
	 * Bluetooth addresses include but are not limited to:<BR>
	 * <code>008037144297</code><BR>
	 * <code>00af8300cd0b</code><BR>
	 * <code>014bd91DA8FC</code>
	 *
	 * @param address  the address of the Bluetooth device as a 12 character
	 * hex string
	 *
	 * @exception NullPointerException if <code>address</code> is
	 * <code>null</code>
	 *
	 * @exception IllegalArgumentException if <code>address</code> is the
	 * address of the local device or is not a valid Bluetooth address
	 */
	protected RemoteDevice(String address) {
		if(address == null) {
            throw new NullPointerException("address is null");
		}
		throw new RuntimeException("Can't initialize bluetooth support");
	}

	/**
	 * Determines if this is a trusted device according to the BCC.
	 *
	 * @return <code>true</code> if the device is a trusted device, otherwise
	 * <code>false</code>
	 */
	public boolean isTrustedDevice() {
		return false;
	}

	/**
	 * Returns the name of this device. The Bluetooth
	 * specification calls this name the "Bluetooth device name" or the
	 * "user-friendly name".  This method will only contact
	 * the remote device if the name is not known or
	 * <code>alwaysAsk</code> is <code>true</code>.
	 *
	 * @param alwaysAsk  if <code>true</code> then the device will be
	 * contacted for its name, otherwise, if there exists a known
	 * name for this device, the name will be returned without
	 * contacting the remote device
	 *
	 * @return the name of the device, or <code>null</code> if the
	 * Bluetooth system does not support this feature; if the local device
	 * is able to contact the remote device, the result will never be
	 * <code>null</code>; if the remote device does not have a name
	 * then an empty string will be returned
	 *
	 * @exception IOException if the remote device can not be contacted or the
	 * remote device could not provide its name
	 */
	public String getFriendlyName(boolean alwaysAsk) throws IOException {
		return null;
	}

	/**
	 * Retrieves the Bluetooth address of this device.  The Bluetooth address
	 * will be 12 characters long.  Valid characters are 0-9 and A-F.  This
	 * method will never return <code>null</code>.
	 *
	 * @return the Bluetooth address of the remote device
	 */
	public final String getBluetoothAddress() {
	    return null;
	}

	/**
	 * Determines if two <code>RemoteDevice</code>s are equal.  Two devices are
	 * equal if they have the same Bluetooth device address.
	 *
	 * @param obj the object to compare to
	 *
	 * @return <code>true</code> if both devices have the same Bluetooth
	 * address; <code>false</code> if both devices do not have the same address;
	 * <code>false</code> if <code>obj</code> is <code>null</code>;
	 * <code>false</code> if <code>obj</code> is not a
	 * <code>RemoteDevice</code>
	 */
	public boolean equals(Object obj) {
		return false;
	}

	/**
	 * Computes the hash code for this object.  This method will return the
	 * same value when it is called multiple times on the same object.
	 *
	 * @return the hash code for this object
	 */
	public int hashCode() {
		return 0;
	}

	/**
	 * Retrieves the Bluetooth device that is at the other end of the Bluetooth
	 * Serial Port Profile connection, L2CAP connection, or OBEX over RFCOMM
	 * connection provided.  This method will never return <code>null</code>.
	 *
	 * @param conn the Bluetooth Serial Port connection, L2CAP connection,
	 * or OBEX over RFCOMM connection whose remote Bluetooth device is needed
	 *
	 * @return the remote device involved in the connection
	 *
	 * @exception IllegalArgumentException if <code>conn</code> is not a
	 * Bluetooth Serial Port Profile connection, L2CAP connection, or OBEX
	 * over RFCOMM connection; if <code>conn</code> is a
	 * <code>L2CAPConnectionNotifier</code>,
	 * <code>StreamConnectionNotifier</code>, or
	 * <code>SessionNotifier</code>
	 *
	 * @exception IOException if the connection is closed
	 *
	 * @exception NullPointerException if <code>conn</code> is
	 * <code>null</code>
	 */
	public static RemoteDevice getRemoteDevice(Connection conn) throws IOException {
		return null;
	}

	/**
	 * Attempts to authenticate this <code>RemoteDevice</code>.
	 * Authentication is a means of verifying the identity of a remote
	 * device. Authentication involves a device-to-device challenge and
	 * response scheme that requires a 128-bit common secret link key
	 * derived from a PIN code shared by both devices. If either side's
	 * PIN code does not match, the authentication process fails and the
	 * method returns <code>false</code>.  The method will also return
	 * <code>false</code> if authentication is incompatible with the
	 * current security settings of the local device established by the
	 * BCC, if the stack does not
	 * support authentication at all, or if the stack does not support
	 * authentication subsequent to connection establishment.
	 *
	 * <p> If this <code>RemoteDevice</code> has previously been
	 * authenticated, then this method returns <code>true</code>
	 * without attempting to re-authenticate this
	 * <code>RemoteDevice</code>.
	 *
	 * @return <code>true</code> if authentication is successful;
	 * otherwise <code>false</code>
	 *
	 * @exception IOException if there are no open connections between
	 * the local device and this <code>RemoteDevice</code>
	 */
	public boolean authenticate() throws IOException {
		return false;
	}

	/**
	 * Determines if this <code>RemoteDevice</code> should be allowed
	 * to continue to access the local service provided by the
	 * <code>Connection</code>.  In Bluetooth, authorization is
	 * defined as the process of deciding if device X is allowed to
	 * access service Y.  The implementation of the
	 * <code>authorize(Connection conn)</code> method asks the
	 * Bluetooth Control Center (BCC) to decide if it is acceptable
	 * for <code>RemoteDevice</code> to continue to access a local
	 * service over the connection <code>conn</code>.  In devices with
	 * a user interface, the BCC is expected to consult with the user
	 * to obtain approval.
	 *
	 * <p> Some Bluetooth systems may allow the user to permanently
	 * authorize a remote device for all local services. When a device
	 * is authorized in this way, it is known as a "trusted device"
	 * -- see {@link #isTrustedDevice() isTrustedDevice()}.
	 *
	 * <p> The <code>authorize()</code> method will also check that the
	 * identity of the <code>RemoteDevice</code> can be verified through authentication.
	 * If this <code>RemoteDevice</code> has been authorized for
	 * <code>conn</code> previously, then this method returns
	 * <code>true</code> without attempting to re-authorize this
	 * <code>RemoteDevice</code>.
	 *
	 * @see #isTrustedDevice
	 *
	 * @param conn the connection that this <code>RemoteDevice</code> is
	 * using to access a local service
	 *
	 * @return <code>true</code> if this <code>RemoteDevice</code> is
	 * successfully authenticated and authorized, otherwise
	 * <code>false</code> if authentication or authorization fails
	 *
	 * @exception IllegalArgumentException if <code>conn</code> is not
	 * a connection to this <code>RemoteDevice</code>, or if the local
	 * device initiated the connection, i.e., the local device is the
	 * client rather than the server.  This exception is also thrown if
	 * <code>conn</code> was created by <code>RemoteDevice</code>
	 * using a scheme other than <code>btspp</code>,
	 * <code>btl2cap</code>, or <code>btgoep</code>. This exception
	 * is thrown if <code>conn</code> is a notifier used by a server
	 * to wait for a client connection, since the notifier is not a
	 * connection to this <code>RemoteDevice</code>.
	 *
	 * @exception IOException if <code>conn</code> is closed
	 */
	public boolean authorize(javax.microedition.io.Connection conn) throws IOException {
		return false;
	}

	/**
	 * Attempts to turn encryption on or off for an existing
	 * connection.  In the case where the parameter <code>on</code> is
	 * <code>true</code>, this method will first authenticate this
	 * <code>RemoteDevice</code> if it has not already been
	 * authenticated.  Then it will attempt to turn on encryption.  If
	 * the connection is already encrypted then this method
	 * returns <code>true</code>.  Otherwise, when the parameter <code>on</code>
	 * is <code>true</code>, either: <UL> <LI> the method succeeds in
	 * turning on encryption for the connection and returns
	 * <code>true</code>, or <LI> the method was unsuccessful in
	 * turning on encryption and returns <code>false</code>.  This
	 * could happen because the stack does not support encryption or
	 * because encryption conflicts with the user's security settings
	 * for the device.  </UL>
	 *
	 * <p> In the case where the parameter <code>on</code> is
	 * <code>false</code>, there are again two possible outcomes: <UL>
	 * <LI> encryption is turned off on the connection and
	 * <code>true</code> is returned, or <LI> encryption is left on
	 * for the connection and <code>false</code> is returned.  </UL>
	 * Encryption may be left on following <code>encrypt(conn,
	 * false)</code> for a variety of reasons.  The user's current
	 * security settings for the device may require encryption or the
	 * stack may not have a mechanism to turn off encryption.  Also,
	 * the BCC may have determined that encryption will be kept on for
	 * the physical link to this <code>RemoteDevice</code>.  The
	 * details of the BCC are implementation dependent, but encryption
	 * might be left on because other connections to the same device
	 * need encryption.  (All of the connections over the same
	 * physical link must be encrypted if any of them are encrypted.)
	 *
	 * <p> While attempting to turn encryption off may not succeed
	 * immediately because other connections need encryption on, there
	 * may be a delayed effect.  At some point, all of the connections
	 * over this physical link needing encryption could be closed or
	 * also have had the method <code>encrypt(conn, false)</code>
	 * invoked for them.  In this case, the BCC may turn off
	 * encryption for all connections over this physical link.  (The
	 * policy used by the BCC is implementation dependent.)  It is
	 * recommended that applications do <code>encrypt(conn,
	 * false)</code> once they no longer need encryption to allow the
	 * BCC to determine if it can reduce the overhead on connections
	 * to this <code>RemoteDevice</code>.
	 *
	 * <p> The fact that <code>encrypt(conn, false)</code> may not
	 * succeed in turning off encryption has very few consequences for
	 * applications.  The stack handles encryption and decryption, so
	 * the application does not have to do anything different
	 * depending on whether the connection is still encrypted or not.
	 *
	 * @param conn the connection whose need for encryption has changed
	 *
	 * @param on  <code>true</code> attempts to turn on encryption;
	 * <code>false</code> attempts to turn off encryption
	 *
	 * @return <code>true</code> if the change succeeded, otherwise
	 * <code>false</code> if it failed
	 *
	 * @exception IOException if <code>conn</code> is closed
	 *
	 * @exception IllegalArgumentException if <code>conn</code> is not
	 * a connection to this <code>RemoteDevice</code>; if <code>conn</code> was
	 * created by the
	 * client side of the connection using a scheme other than
	 * <code>btspp</code>, <code>btl2cap</code>, or
	 * <code>btgoep</code> (for example, this exception will be
	 * thrown if <code>conn</code> was created using the
	 * <code>file</code> or <code>http</code> schemes.);
	 * if <code>conn</code> is a notifier used by a server
	 * to wait for a client connection, since the notifier is not a
	 * connection to this <code>RemoteDevice</code>
	 */
	public boolean encrypt(javax.microedition.io.Connection conn, boolean on) throws IOException {
		return false;
	}

	/**
	 * Determines if this <code>RemoteDevice</code> has been
	 * authenticated.
	 * <P>
	 * A device may have been authenticated by this application
	 * or another application.  Authentication applies to an ACL link between
	 * devices and not on a specific L2CAP, RFCOMM, or OBEX connection.
	 * Therefore, if <code>authenticate()</code> is performed when an L2CAP
	 * connection is made to device A, then <code>isAuthenticated()</code> may
	 * return <code>true</code> when tested as part of making an RFCOMM
	 * connection to device A.
	 *
	 * @return <code>true</code> if this <code>RemoteDevice</code> has
	 * previously been authenticated; <code>false</code> if it has not
	 * been authenticated or there are no open connections between the
	 * local device and this <code>RemoteDevice</code>
	 */
	public boolean isAuthenticated() {
		return false;
	}

	/**
	 * Determines if this <code>RemoteDevice</code> has been
	 * authorized previously by the BCC of the local device to
	 * exchange data related to the service associated with the
	 * connection. Both clients and servers can call this method.
	 * However, for clients this method returns <code>false</code> for
	 * all legal values of the <code>conn</code> argument.
	 *
	 * @param conn a connection that this <code>RemoteDevice</code> is
	 * using to access a service or provide a service
	 *
	 * @return <code>true</code> if <code>conn</code> is a server-side
	 * connection and this <code>RemoteDevice</code> has been
	 * authorized; <code>false</code> if <code>conn</code> is a
	 * client-side connection, or a server-side connection that has not
	 * been authorized
	 *
	 * @exception IllegalArgumentException if <code>conn</code> is not
	 * a connection to this <code>RemoteDevice</code>; if
	 * <code>conn</code> was not created using one of the schemes
	 * <code>btspp</code>, <code>btl2cap</code>, or
	 * <code>btgoep</code>; or if <code>conn</code> is a notifier
	 * used by a server to wait for a client connection, since the
	 * notifier is not a connection to this <code>RemoteDevice</code>.
	 *
	 * @exception IOException if <code>conn</code> is closed
	 */
	public boolean isAuthorized(javax.microedition.io.Connection conn) throws IOException {
		return false;
	}

	/**
	 * Determines if data exchanges with this <code>RemoteDevice</code>
	 * are currently being encrypted.
	 * <P>
	 * Encryption may have been previously turned on by this or another
	 * application.  Encryption applies to an ACL link
	 * between devices and not on a specific L2CAP, RFCOMM, or OBEX connection.
	 * Therefore, if <code>encrypt()</code> is performed with the
	 * <code>on</code> parameter set to <code>true</code> when an L2CAP
	 * connection is made to device A, then <code>isEncrypted()</code> may
	 * return <code>true</code> when tested as part of making an RFCOMM
	 * connection to device A.
	 *
	 * @return <code>true</code> if data exchanges with this
	 * <code>RemoteDevice</code> are being encrypted; <code>false</code>
	 * if they are not being encrypted, or there are no open connections
	 * between the local device and this <code>RemoteDevice</code>
	 */
	public boolean isEncrypted() {
		return false;
	}

}
