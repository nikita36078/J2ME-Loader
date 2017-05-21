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
 * 
 * The {@code ServiceRecord} interface describes characteristics of a Bluetooth 
 * service. A {@code ServiceRecord} contains a set of service attributes, where 
 * each service attribute is an (ID, value) pair. A Bluetooth attribute 
 * ID is a 16-bit unsigned integer, and an attribute value is a 
 * {@link javax.bluetooth.DataElement}.
 * <p>
 * The structure and use of service records is specified by the Bluetooth 
 * specification in the Service Discovery Protocol (SDP) document. Most of 
 * the Bluetooth Profile specifications also describe the structure of the
 * service records used by the Bluetooth services that conform to the profile.
 * <p>
 * An SDP Server maintains a Service Discovery Database (SDDB) of service 
 * records that describe the services on the local device. Remote SDP 
 * clients can use the SDP to query an SDP server for any service records 
 * of interest. A service record provides sufficient information to allow 
 * an SDP client to connect to the Bluetooth service on the SDP server's device.
 * <p>
 * {@code ServiceRecords} are made available to a client application via an argument 
 * of the {@link javax.bluetooth.DiscoveryListener#servicesDiscovered} method
 * of the {@link javax.bluetooth.DiscoveryListener} interface. 
 * {@code ServiceRecords} are available to server applications via the method 
 * {@link javax.bluetooth.LocalDevice#getRecord(javax.microedition.io.Connection)} on {@link javax.bluetooth.LocalDevice}.
 * <p>
 * There might be many service attributes in a service record, and the SDP 
 * protocol makes it possible to specify the subset of the service 
 * attributes that an SDP client wants to retrieve from a remote service 
 * record. The {@code ServiceRecord} interface treats certain service attribute 
 * IDs as default IDs, and, if present, these service attributes are 
 * automatically retrieved during service searches.
 * <p>
 * The Bluetooth Assigned Numbers document 
 * (<a href="http://www.bluetooth.org/assigned-numbers/sdp.htm">
 * http://www.bluetooth.org/assigned-numbers/sdp.htm</a>) defines a large 
 * number of service attribute IDs. Here is a subset of the most common 
 * service attribute IDs and their types.
 *
 * <TABLE BORDER=1>
 * <TR><TH>Attribute Name</TH><TH>Attribute ID</TH><TH>Attribute Value Type</TH></TR>
 * <TR><TD>ServiceRecordHandle</TD><TD>0x0000</TD><TD>32-bit unsigned integer</TD></TR>
 * <TR><TD>ServiceClassIDList</TD><TD>0x0001</TD><TD>DATSEQ of UUIDs</TD></TR>
 * <TR><TD>ServiceRecordState</TD><TD>0x0002</TD><TD>32-bit unsigned integer</TD></TR>
 * <TR><TD>ServiceID</TD><TD>0x0003</TD><TD>UUID</TD></TR>
 * <TR><TD>ProtocolDescriptorList</TD><TD>0x0004</TD><TD>DATSEQ of DATSEQ of UUID and optional parameters</TD></TR>
 * <TR><TD>BrowseGroupList</TD><TD>0x0005</TD><TD>DATSEQ of UUIDs</TD></TR>
 * <TR><TD>LanguageBasedAttributeIDList</TD><TD>0x0006</TD><TD>DATSEQ of DATSEQ triples</TD></TR>
 * <TR><TD>ServiceInfoTimeToLive</TD><TD>0x0007</TD><TD>32-bit unsigned integer</TD></TR>
 * <TR><TD>ServiceAvailability</TD><TD>0x0008</TD><TD>8-bit unsigned integer</TD></TR>
 * <TR><TD>BluetoothProfileDescriptorList</TD><TD>0x0009</TD><TD>DATSEQ of DATSEQ pairs</TD></TR>
 * <TR><TD>DocumentationURL</TD><TD>0x000A</TD><TD>URL</TD></TR>
 * <TR><TD>ClientExecutableURL</TD><TD>0x000B</TD><TD>URL</TD></TR>
 * <TR><TD>IconURL</TD><TD>0x000C</TD><TD>URL</TD></TR>
 * <TR><TD>VersionNumberList</TD><TD>0x0200</TD><TD><code>DATSEQ</code> of 16-bit unsigned integers</TD></TR>
 * <TR><TD>ServiceDatabaseState</TD><TD>0x0201</TD><TD>32-bit unsigned integer</TD></TR>
 * </TABLE>
 * <p>
 * The following table lists the common string-valued attribute ID offsets used in a 
 * {@code ServiceRecord}. These offsets must be added to a base value to obtain the actual 
 * service ID. (For more information, see the Service Discovery Protocol Specification 
 * located in the Bluetooth Core Specification at <a href=
 * "http://www.bluetooth.com/dev/specifications.asp">http://www.bluetooth.com/dev/specifications.asp
 * </a>).
 * <p>
 * <TABLE BORDER=1> 
 * <TR><TH>Attribute Name</TH><TH>Attribute ID Offset</TH><TH>Attribute Value Type</TH></TR>
 * <TR><TD>ServiceName</TD><TD>0x0000</TD><TD>String</TD></TR>
 * <TR><TD>ServiceDescription</TD><TD>0x0001</TD><TD>String</TD></TR>
 * <TR><TD>ProviderName</TD><TD>0x0002</TD><TD>String</TD></TR>
 * </TABLE>
 */
public interface ServiceRecord {

	/**
	 * Authentication and encryption are not needed on a connection to this
	 * service. Used with {@link #getConnectionURL(int, boolean)} method. 
	 * <p>
	 * {@code NOAUTHENTICATE_NOENCRYPT} is set to the constant value 0x00 (0).
	 * 
	 * @see #getConnectionURL(int, boolean)
	 */
	public static final int NOAUTHENTICATE_NOENCRYPT = 0;

	/**
	 * Authentication is required for connections to this service, but not
	 * encryption. It is OK for encryption to be either on or off for the
	 * connection. Used with {@link #getConnectionURL(int, boolean)} method. 
	 * <p>
	 * {@code AUTHENTICATE_NOENCRYPT} is set to the constant value 0x01 (1).
	 * 
	 * @see #getConnectionURL(int, boolean)
	 */
	public static final int AUTHENTICATE_NOENCRYPT = 1;

	/**
	 * Authentication and encryption are required for connections to this
	 * service. Used with {@link #getConnectionURL(int, boolean)} method. 
	 * <p>
	 * {@code AUTHENTICATE_ENCRYPT} is set to the constant value 0x02 (2).
	 */
	public static final int AUTHENTICATE_ENCRYPT = 2;

	/**
	 * Returns the value of the service attribute ID provided it is present in
	 * the service record, otherwise this method returns {@code null}. 
	 * 
	 * @param attrID the attribute whose value is to be returned 
	 * @return the value of the attribute ID if present in the service record, 
	 * 				otherwise {@code null}
	 * @throws IllegalArgumentException if {@code attrID} is negative or greater than
	 * 										or equal to 2<sup>16</sup>
	 */
	public DataElement getAttributeValue(int attrID);

	/**
	 * Returns the remote Bluetooth device that populated the service record
	 * with attribute values. It is important to note that the Bluetooth device
	 * that provided the value might not be reachable anymore, since it can
	 * move, turn off, or change its security mode denying all further
	 * transactions. 
	 * 
	 * @return the remote Bluetooth device that populated the
	 * service record, or {@code null} if the local device populated this {@code ServiceRecord}
	 */
	public RemoteDevice getHostDevice();

	/**
	 * Returns the service attribute IDs whose value could be retrieved by a
	 * call to {@link #getAttributeValue(int)}. The list of attributes being returned is not
	 * sorted and includes default attributes. 
	 * 
	 * @return an array of service
	 * attribute IDs that are in this object and have values for them; if there
	 * are no attribute IDs that have values, this method will return an array
	 * of length zero. 
	 * @see #getAttributeValue(int)
	 */
	public int[] getAttributeIDs();

	/**
	 * Retrieves the values by contacting the remote Bluetooth device for a set
	 * of service attribute IDs of a service that is available on a Bluetooth
	 * device. (This involves going over the air and contacting the remote
	 * device for the attribute values.) The system might impose a limit on the
	 * number of service attribute ID values one can request at a time.
	 * Applications can obtain the value of this limit as a String by calling
	 * {@code LocalDevice.getProperty("bluetooth.sd.attr.retrievable.max")}. The method
	 * is blocking and will return when the results of the request are
	 * available. Attribute IDs whose values could be obtained are added to this
	 * service record. If there exist attribute IDs for which values are
	 * retrieved this will cause the old values to be overwritten. If the remote
	 * device cannot be reached, an {link java.lang.IOException} will be thrown. 
	 * 
	 * @param attrIDs  the list of service attributes IDs whose value are to be
	 * retrieved; the number of attributes cannot exceed the property
	 * {@code bluetooth.sd.attr.retrievable.max}; the attributes in the request must be
	 * legal, i.e. their values are in the range of [0, 2<sup>16</sup>-1]. The input
	 * attribute IDs can include attribute IDs from the default attribute set
	 * too. 
	 * @return {@code true} if the request was successful in retrieving values for
	 * some or all of the attribute IDs; {@code false} if it was unsuccessful in
	 * retrieving any values
	 * @throws java.io.IOException if the local device
	 * is unable to connect to the remote Bluetooth device that was the source
	 * of this {@code ServiceRecord}; if this {@code ServiceRecord} was deleted from the SDDB of
	 * the remote device 
	 * @throws java.lang.IllegalArgumentException  if the size of {@code attrIDs}
	 * exceeds the system specified limit as defined by
	 * {@code bluetooth.sd.attr.retrievable.max}; if the {@code attrIDs} array length is zero;
	 * if any of their values are not in the range of [0, 2^<sup>16</sup>-1]; if {@code attrIDs}
	 * has duplicate values 
	 * @throws java.lang.NullPointerException  if {@code attrIDs} is {@code null}
	 * @throws java.lang.RuntimeException  if this {@code ServiceRecord} describes a service on the local
	 * device rather than a service on a remote device
	 */
	public boolean populateRecord(int[] attrIDs) throws IOException;

	/**
	 * Returns a {@link java.lang.String} including optional parameters that can
	 * be used by a client to connect to the service described by this
	 * {@code ServiceRecord}. The return value can be used as the first
	 * argument to
	 * {@link javax.microedition.io.Connector#open(String, int, boolean)}. In
	 * the case of a Serial Port service record, this string might look like
	 * {@code "btspp://0050CD00321B:3;authenticate=true;encrypt=false;master=true"},
	 * where "0050CD00321B" is the Bluetooth address of the device that provided
	 * this {@code ServiceRecord}, "3" is the RFCOMM server channel mentioned
	 * in this {@code ServiceRecord}, and there are three optional parameters
	 * related to security and master/slave roles.
	 * <p>
	 * If this method is called on a {@code ServiceRecord} returned from
	 * {@link javax.bluetooth.LocalDevice#getRecord(javax.microedition.io.Connection)},
	 * it will return the connection string that a remote device will use to
	 * connect to this service.
	 * 
	 * @param requiredSecurity
	 *            determines whether authentication or encryption are required
	 *            for a connection
	 * @param mustBeMaster
	 *            {@code true} indicates that this device must play the role of
	 *            master in connections to this service; {@code false} indicates
	 *            that the local device is willing to be either the master or
	 *            the slave
	 * @return a {@link java.lang.String} that can be used to connect to the
	 *         service or {@code null} if the ProtocolDescriptorList in this
	 *         {@code ServiceRecord} is not formatted according to the Bluetooth
	 *         specification
	 * @throws java.lang.IllegalArgumentException
	 *             if requiredSecurity is not one of the constants
	 *             {@code NOAUTHENTICATE_NOENCRYPT},
	 *             {@code AUTHENTICATE_NOENCRYPT}, or
	 *             {@code AUTHENTICATE_ENCRYPT}
	 * @see #AUTHENTICATE_ENCRYPT
	 * @see #NOAUTHENTICATE_NOENCRYPT
	 * @see #AUTHENTICATE_NOENCRYPT
	 */
	public String getConnectionURL(int requiredSecurity, boolean mustBeMaster);

	/**
	 * Used by a server application to indicate the major service class bits
	 * that should be activated in the server's DeviceClass when this
	 * {@code ServiceRecord} is added to the SDDB. When client devices do device
	 * discovery, the server's DeviceClass is provided as one of the arguments
	 * of the
	 * {@link javax.bluetooth.DiscoveryListener#deviceDiscovered(RemoteDevice, DeviceClass)}
	 * method of the {@link javax.bluetooth.DiscoveryListener} interface. Client
	 * devices can consult the DeviceClass of the server device to get a general
	 * idea of the kind of device this is (e.g., phone, PDA, or PC) and the
	 * major service classes it offers (e.g., rendering, telephony, or
	 * information). A server application should use the
	 * {@link #setDeviceServiceClasses(int)} method to describe its service in
	 * terms of the major service classes. This allows clients to obtain a
	 * DeviceClass for the server that accurately describes all of the services
	 * being offered.
	 * <p>
	 * 
	 * When {@code acceptAndOpen()} is
	 * invoked for the first time on the notifier associated with this
	 * {@code ServiceRecord}, the classes argument from the
	 * {@link #setDeviceServiceClasses(int)} method is OR'ed with the current
	 * setting of the major service class bits of the local device. The OR
	 * operation potentially activates additional bits. These bits may be
	 * retrieved by calling {@link javax.bluetooth.LocalDevice#getDeviceClass()}
	 * on the {@link javax.bluetooth.LocalDevice} object. Likewise, a call to
	 * {@link javax.bluetooth.LocalDevice#updateRecord(ServiceRecord)} will
	 * cause the major service class bits to be OR'ed with the current settings
	 * and updated.
	 * <p>
	 * 
	 * The documentation for {@link javax.bluetooth.DeviceClass} gives examples
	 * of the integers that describe each of the major service classes and
	 * provides a URL for the complete list. These integers can be used
	 * individually or OR'ed together to describe the appropriate value for
	 * classes.
	 * <p>
	 * 
	 * Later, when this {@code ServiceRecord} is removed from the SDDB, the
	 * implementation will automatically deactivate the device bits that were
	 * activated as a result of the call to setDeviceServiceClasses. The only
	 * exception to this occurs if there is another {@code ServiceRecord} that
	 * is in the SDDB and {@link #setDeviceServiceClasses(int)} has been sent to
	 * that other {@code ServiceRecord} to request that some of the same bits be
	 * activated.
	 * 
	 * @param classes
	 *            an integer whose binary representation indicates the major
	 *            service class bits that should be activated
	 * @throws java.lang.IllegalArgumentException
	 *             if {@code classes} is not an OR of one or more of the major
	 *             service class integers in the Bluetooth Assigned Numbers
	 *             document. While Limited Discoverable Mode is included in this
	 *             list of major service classes, its bit is activated by
	 *             placing the device in Limited Discoverable Mode (see the GAP
	 *             specification), so if bit 13 is set this exception will be
	 *             thrown.
	 * @throws java.lang.RuntimeException -
	 *             if the {@code ServiceRecord} receiving the message was
	 *             obtained from a remote device
	 */
	public void setDeviceServiceClasses(int classes);

	/**
	 * Modifies this {@code ServiceRecord} to contain the service attribute
	 * defined by the attribute-value pair ({@code attrID}, {@code attrValue}).
	 * If the attrID does not exist in the {@code ServiceRecord}, this
	 * attribute-value pair is added to this {@code ServiceRecord} object. If
	 * the {@code attrID} is already in this {@code ServiceRecord}, the value
	 * of the attribute is changed to {@code attrValue}. If {@code attrValue}
	 * is null, the attribute with the attribute ID of {@code attrID} is removed
	 * from this {@code ServiceRecord} object. If attrValue is null and attrID
	 * does not exist in this object, this method will return false.
	 * <p>
	 * 
	 * This method makes no modifications to a service record in the SDDB. In
	 * order for any changes made by this method to be reflected in the SDDB, a
	 * call must be made to the acceptAndOpen() method of the associated
	 * notifier to add this {@code ServiceRecord} to the SDDB for the first
	 * time, or a call must be made to the updateRecord() method of LocalDevice
	 * to modify the version of this {@code ServiceRecord} that is already in
	 * the SDDB.
	 * <p>
	 * 
	 * This method prevents the {@code ServiceRecordHandle} from being modified
	 * by throwing an IllegalArgumentException.
	 * 
	 * @param attrID
	 *            the service attribute ID
	 * @param attrValue
	 *            the {@link javax.bluetooth.DataElement} which is the value of
	 *            the service attribute
	 * @return {@code true} if the service attribute was successfully added,
	 *         removed, or modified; {@code false} if {@code attrValue} is
	 *         {@code null} and {@code attrID} is not in this object
	 * @throws java.lang.IllegalArgumentException
	 *             if {@code attrID} does not represent a 16-bit unsigned
	 *             integer; if {@code attrID} is the value of
	 *             {@code ServiceRecord}Handle (0x0000)
	 * @throws java.lang.RuntimeException
	 *             if this method is called on a {@code ServiceRecord} that was
	 *             created by a call to
	 *             {@link javax.bluetooth.DiscoveryAgent#searchServices(int[], UUID[], RemoteDevice, DiscoveryListener)}
	 */
	public boolean setAttributeValue(int attrID, DataElement attrValue);

}