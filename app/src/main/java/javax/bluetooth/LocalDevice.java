/**
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *
 *
 *  @version $Id$
 */ 

package javax.bluetooth;

import javax.microedition.io.Connection;

/**
 * The <code>LocalDevice</code> class defines the basic functions of the
 * Bluetooth manager.  The
 * Bluetooth manager provides the lowest level of interface possible
 * into the Bluetooth stack.  It provides access to and control of the
 * local Bluetooth device.
 * <P>
 * This class produces a singleton object.
 *
 * @version 1.0 February 11, 2002
 * @version 1.1 September 2, 2005
 */
public class LocalDevice {

	/**
	 * The default constructor is hidden so that no one can create a new
	 * instance of the LocalDevice.  To get the LocalDevice
	 * object for this device, use the <code>getLocalDevice()</code>
	 * static method in this class.
	 *
	 * @see #getLocalDevice
	 */
	private LocalDevice() throws BluetoothStateException {
	}

	/**
	 * Retrieves the <code>LocalDevice</code> object for the local Bluetooth
	 * device. Multiple calls to this method will return the same
	 * object. This method will never return <code>null</code>.
	 *
	 * @return  an object that represents the local Bluetooth device
	 *
	 * @exception BluetoothStateException if the Bluetooth system could not be
	 * initialized
	 */
	public static LocalDevice getLocalDevice() throws BluetoothStateException {
		throw new BluetoothStateException("This is not JSR-82 implementation");
	}

	/**
	 * Returns the discovery agent for this device.  Multiple calls
	 * to this method will return the same object.  This method will
	 * never return <code>null</code>.
	 *
	 * @return the discovery agent for the local device
	 *
	 */
	public DiscoveryAgent getDiscoveryAgent() {
		return null;
	}

	/**
	 * Retrieves the name of the local device.  The Bluetooth
	 * specification calls this name the "Bluetooth device name" or the
	 * "user-friendly name".
	 *
	 * @return the name of the local device; <code>null</code> if the
	 * name could not be retrieved
	 */
	public String getFriendlyName() {
	    return null;
	}

	/**
	 * Retrieves the <code>DeviceClass</code> object that represents the
	 * service classes, major device class, and minor device class of the
	 * local device.  This method will return <code>null</code> if the
	 * service classes, major device class, or minor device class could not
	 * be determined.
	 *
	 * @return the service classes, major device class, and minor device
	 * class of the local device, or <code>null</code> if the service
	 * classes, major device class or minor device class could not be
	 * determined
	 *
	 */
	public DeviceClass getDeviceClass() {
	    return null;
	}

	/**
	 * Sets the discoverable mode of the device.  The <code>mode</code> may be
	 * any number in the range 0x9E8B00 to 0x9E8B3F as defined by the Bluetooth
	 * Assigned Numbers Document.  When this specification was defined, only GIAC
	 * (<code>DiscoveryAgent.GIAC</code>) and
	 * LIAC (<code>DiscoveryAgent.LIAC</code>) were defined, but Bluetooth profiles
	 * may add additional access codes in the future.  To determine what values
	 * may be used, check the Bluetooth Assigned Numbers document at
	 * <A HREF="http://www.bluetooth.org/assigned-numbers/baseband.htm">
	 * http://www.bluetooth.org/assigned-numbers/baseband.htm</A>.  If
	 * <code>DiscoveryAgent.GIAC</code> or <code>DiscoveryAgent.LIAC</code> are
	 * provided, then this method will attempt to put the device into general or
	 * limited discoverable mode, respectively.  To take a device out of
	 * discoverable mode, provide the
	 * <code>DiscoveryAgent.NOT_DISCOVERABLE</code> flag.  The BCC decides if the
	 * request will be granted.  In addition to the BCC, the Bluetooth system
	 * could effect the discoverability of a device.
	 * <P>
	 * According to the Bluetooth Specification, a device should only be
	 * limited discoverable (<code>DiscoveryAgent.LIAC</code>) for 1 minute.
	 * This is handled by the implementation of the API.  After the minute is
	 * up, the device will revert back to the previous discoverable mode.
	 *
	 * @see DiscoveryAgent#GIAC
	 * @see DiscoveryAgent#LIAC
	 * @see DiscoveryAgent#NOT_DISCOVERABLE
	 *
	 * @param mode  the mode the device should be in; valid modes are
	 * <code>DiscoveryAgent.GIAC</code>, <code>DiscoveryAgent.LIAC</code>,
	 * <code>DiscoveryAgent.NOT_DISCOVERABLE</code> and any value in the
	 * range 0x9E8B00 to 0x9E8B3F
	 *
	 * @return <code>true</code> if the request succeeded, otherwise
	 * <code>false</code> if the request failed because the BCC denied
	 * the request; <code>false</code> if the Bluetooth system does not
	 * support the access mode specified in <code>mode</code>
	 *
	 * @exception IllegalArgumentException if the <code>mode</code> is
	 * not <code>DiscoveryAgent.GIAC</code>, <code>DiscoveryAgent.LIAC</code>,
	 * <code>DiscoveryAgent.NOT_DISCOVERABLE</code>, or in the range
	 * 0x9E8B00 to 0x9E8B3F
	 *
	 * @exception BluetoothStateException if the Bluetooth system is in
	 * a state that does not allow the discoverable mode to be changed
	 *
	 */
	public boolean setDiscoverable(int mode) throws BluetoothStateException {
		if ((mode != DiscoveryAgent.GIAC) && (mode != DiscoveryAgent.LIAC) && (mode != DiscoveryAgent.NOT_DISCOVERABLE) 
				&& (mode < 0x9E8B00 || mode > 0x9E8B3F)) {
			throw new IllegalArgumentException("Invalid discoverable mode");
		}
		return false;
	}

	/**
	 * Retrieves the power state of the local Bluetooth device.
	 * 
	 * @since 1.1
	 * @return <code>true</code> if the local Bluetooth device is powered on,
	 *         <code>false</code> if the local Bluetooth device is off.
	 */
	public static boolean isPowerOn() {
		return false;
	}

	/**
	 * Retrieves the local device's discoverable mode.  The return value
	 * will be <code>DiscoveryAgent.GIAC</code>,
	 * <code>DiscoveryAgent.LIAC</code>,
	 * <code>DiscoveryAgent.NOT_DISCOVERABLE</code>, or a value in the
	 * range 0x9E8B00 to 0x9E8B3F.
	 *
	 * @see DiscoveryAgent#GIAC
	 * @see DiscoveryAgent#LIAC
	 * @see DiscoveryAgent#NOT_DISCOVERABLE
	 *
	 * @return the discoverable mode the device is presently in
	 */
	public int getDiscoverable() {
		return DiscoveryAgent.NOT_DISCOVERABLE;
	}

	/**
	 * Retrieves Bluetooth system properties.  The following properties
	 * must be supported, but additional values are allowed:
	 * <TABLE>
	 * <TR><TH>Property Name</TH><TH>Description</TH></TR>
	 * <TR><TD>bluetooth.api.version</TD><TD>The version of the Java
	 * API for Bluetooth wireless technology that is supported. For this
	 * version it will be set to "1.0".</TD></TR>
	 * <TR><TD>bluetooth.master.switch</TD><TD>Is master/slave switch
	 * allowed?
	 * Valid values are either "true" or "false".</TD></TR>
	 * <TR><TD>bluetooth.sd.attr.retrievable.max</TD><TD>Maximum number of
	 * service attributes to be retrieved per service record. The string
	 * will be in Base 10 digits.</TD></TR>
	 * <TR><TD>bluetooth.connected.devices.max</TD><TD>The maximum number
	 * of connected devices supported. This number may be greater than 7 if
	 * the implementation handles parked connections.  The string will be
	 * in Base 10 digits. </TD></TR>
	 * <TR><TD>bluetooth.l2cap.receiveMTU.max</TD><TD>The maximum ReceiveMTU
	 * size in bytes supported in L2CAP. The string will be in Base 10
	 * digits, e.g. "32".</TD></TR>
	 * <TR><TD>bluetooth.sd.trans.max</TD><TD>Maximum
	 * number of concurrent service discovery transactions. The string
	 * will be in Base 10 digits. </TD></TR>
	 * <TR><TD>bluetooth.connected.inquiry.scan</TD><TD>Is Inquiry scanning allowed
	 * during connection? Valid values are either "true" or "false".</TD></TR>
	 * <TR><TD>bluetooth.connected.page.scan</TD><TD>Is Page
	 * scanning allowed during connection? Valid values are either "true"
	 * or "false".</TD></TR>
	 * <TR><TD>bluetooth.connected.inquiry</TD><TD>Is Inquiry allowed during a
	 * connection?  Valid values are either "true" or "false".</TD></TR>
	 * <TR><TD>bluetooth.connected.page</TD><TD>Is paging allowed during a
	 * connection?  In other words, can a connection be established to one
	 * device if it is already connected to another device.  Valid values are
	 * either "true" or "false".</TD></TR>
	 * </TABLE>
     * <p>
	 * BlueCove specific properties.
	 * <TABLE>
	 * <TR><TH>Property Name</TH><TH>Description</TH></TR>
	 * <TR><TD>bluecove</TD><TD>The version of BlueCove implementation</TD></TR>
	 * <TR><TD>bluecove.stack</TD><TD>The Bluetooth Stack: "winsock", "widcomm" or "bluesoleil"</TD></TR>
	 * </TABLE>
     *
	 * @param property the property to retrieve as defined in this class.
	 *
	 * @return the value of the property specified; <code>null</code> if
	 * the <code>property</code> is not defined
	 */
	public static String getProperty(String property) {
		return null;
	}

	/**
	 * Retrieves the Bluetooth address of the local device.  The
	 * Bluetooth address will never be <code>null</code>.  The Bluetooth address
	 * will be 12 characters long.  Valid characters are 0-9 and A-F.
	 *
	 * @return the Bluetooth address of the local device
	 */
	public String getBluetoothAddress() {
	    return null;
	}

	/**
	 * Gets the service record corresponding to a <code>btspp</code>,
	 * <code>btl2cap</code>, or <code>btgoep</code> notifier.  In the
	 * case of a run-before-connect service, the service record
	 * returned by <code>getRecord()</code> was created by the same
	 * call to <code>Connector.open()</code> that created the
	 * <code>notifier</code>.
	 *
	 * <p> If a connect-anytime server application does not already
	 * have a service record in the SDDB, either because a service
	 * record for this service was never added to the SDDB or because
	 * the service record was added and then removed, then the
	 * <code>ServiceRecord</code> returned by <code>getRecord()</code>
	 * was created by the same call to <code>Connector.open()</code>
	 * that created the notifier.
	 * <p> In the case of a connect-anytime service, there may be a
	 * service record in the SDDB corresponding to this service prior
	 * to application startup.  In this case, the
	 * <code>getRecord()</code> method must return a
	 * <code>ServiceRecord</code> whose contents match those of the
	 * corresponding service record in the SDDB.  If a connect-anytime
	 * server application made changes previously to its service record
	 * in the SDDB (for example, during a previous execution of the
	 * server), and that service record is still in the SDDB, then
	 * those changes must be reflected in the
	 * <code>ServiceRecord</code> returned by <code>getRecord()</code>.
	 *
	 * <p> Two invocations of this method with the same
	 * <code>notifier</code> argument return objects that describe the
	 * same service attributes, but the return values may be different
	 * object references.
	 *
	 * @param notifier a connection that waits for clients to connect
	 * to a Bluetooth service
	 *
	 * @return the <code>ServiceRecord</code> associated with
	 * <code>notifier</code>
	 *
	 * @exception IllegalArgumentException if <code>notifier</code> is
	 * closed, or if <code>notifier</code> does not implement one of
	 * the following interfaces:
	 * <code>javax.microedition.io.StreamConnectionNotifier</code>,
	 * <code>javax.bluetooth.L2CapConnectionNotifier</code>, or
	 * <code>javax.obex.SessionNotifier</code>.  This exception is
	 * also thrown if <code>notifier</code> is not a Bluetooth
	 * notifier, e.g., a <code>StreamConnectionNotifier</code> created
	 * with a scheme other than <code>btspp</code>.
	 *
	 * @exception NullPointerException if <code>notifier</code> is
	 * <code>null</code>
	 *
	 */
	public ServiceRecord getRecord(Connection notifier) {
		if (notifier == null) {
			throw new NullPointerException("notifier is null");
		}

		return null;
	}

	/**
	 * Updates the service record in the local SDDB that corresponds
	 * to the <code>ServiceRecord</code> parameter.  Updating is
	 * possible only if <code>srvRecord</code> was obtained using the
	 * <code>getRecord()</code> method. The service record in the SDDB
	 * is modified to have the same service attributes with the same
	 * contents as <code>srvRecord</code>.
	 *
	 * <p>
	 *
	 * If <code>srvRecord</code> was obtained from the SDDB of a
	 * remote device using the service search methods, updating is not
	 * possible and this method will throw an
	 * <code>IllegalArgumentException</code>.
	 *
	 * <P>
	 *
	 * If the <code>srvRecord</code> parameter is a <code>btspp</code>
	 * service record, then before the SDDB is changed the following
	 * checks are performed. If any of these checks fail, then an
	 * <code>IllegalArgumentException</code> is thrown.
	 *
	 * <UL>
	 * <LI>ServiceClassIDList and ProtocolDescriptorList, the mandatory
	 * service attributes for a <code>btspp</code> service record, must
	 * be present in <code>srvRecord</code>.
	 * <LI>L2CAP and RFCOMM must be in the ProtocolDescriptorList.
	 * <LI><code>srvRecord</code> must not have changed the RFCOMM server
	 *  channel number from the channel number that is currently in the
	 *  SDDB version of this service record.
	 * </UL>
	 *
	 * <P>
	 *
	 * If the <code>srvRecord</code> parameter is a <code>btl2cap</code>
	 * service record, then before the SDDB is changed the following
	 * checks are performed. If any of these checks fail, then an
	 * <code>IllegalArgumentException</code> is thrown.
	 * <UL>
	 * <LI>ServiceClassIDList and ProtocolDescriptorList, the mandatory
	 * service attributes for a <code>btl2cap</code> service record,
	 * must be present in <code>srvRecord</code>.
	 * <LI>L2CAP must be in the ProtocolDescriptorList.
	 * <LI><code>srvRecord</code> must not have changed the PSM value
	 * from the PSM value that is currently in the SDDB version of this
	 * service record.
	 * </UL>
	 *
	 * <P>
	 *
	 * If the <code>srvRecord</code> parameter is a <code>btgoep</code>
	 * service record, then before the SDDB is changed the following
	 * checks are performed. If any of these checks fail, then an
	 * <code>IllegalArgumentException</code> is thrown.
	 * <UL>
	 * <LI>ServiceClassIDList and ProtocolDescriptorList, the mandatory
	 * service attributes for a <code>btgoep</code> service record, must
	 * be present in <code>srvRecord</code>.
	 * <LI>L2CAP, RFCOMM and OBEX must all be in the
	 * ProtocolDescriptorList.
	 * <LI><code>srvRecord</code> must not have changed the RFCOMM server
	 *  channel number from the channel number that is currently in the
	 *  SDDB version of this service record.
	 * </UL>
	 *
	 * <p>
	 *
	 * <code>updateRecord()</code> is not required to ensure that
	 * <code>srvRecord</code> is a completely valid service record. It
	 * is the responsibility of the application to ensure that
	 * <code>srvRecord</code> follows all of the applicable syntactic
	 * and semantic rules for service record correctness.
	 *
	 * <P>
	 *
	 * If there is currently no SDDB version of the
	 * <code>srvRecord</code> service record, then this method will do
	 * nothing.
	 *
	 * @param srvRecord the new contents to use for the service record in
	 * the SDDB
	 *
	 * @exception NullPointerException if <code>srvRecord</code> is
	 * <code>null</code>
	 *
	 * @exception IllegalArgumentException if the structure of the
	 * <code>srvRecord</code> is  missing any mandatory service
	 * attributes, or if an attempt has been made to change any of the
	 * values described as fixed.
	 *
	 * @exception ServiceRegistrationException if the local SDDB could
	 * not be updated successfully due to
	 * insufficient disk space, database locks, etc.
	 * 
	 */
	public void updateRecord(ServiceRecord srvRecord) throws ServiceRegistrationException {
		if (srvRecord == null) {
			throw new NullPointerException("Service Record is null");
		}
	}

}