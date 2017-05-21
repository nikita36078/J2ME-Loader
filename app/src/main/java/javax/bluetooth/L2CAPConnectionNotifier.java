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
 * The <code>L2CAPConnectionNotifier</code> interface provides
 * an L2CAP connection notifier.
 * <P>
 * To create a server connection, the protocol must be
 * <code>btl2cap</code>. The target contains "localhost:" and the UUID of the
 * service. The parameters are ReceiveMTU and  TransmitMTU, the same parameters
 * used to define a client connection. Here is an example of a valid server connection
 * string:<BR>
 * <code>btl2cap://localhost:3B9FA89520078C303355AAA694238F07;ReceiveMTU=512;TransmitMTU=512</code><BR>
 * <P>
 * A call to Connector.open() with this string will return a
 * <code>javax.bluetooth.L2CAPConnectionNotifier</code> object. An
 * <code>L2CAPConnection</code> object is obtained from the
 * <code>L2CAPConnectionNotifier</code> by calling the method
 * <code>acceptAndOpen()</code>.
 *
 * @version 1.0 February 11, 2002
 *
 */
public interface L2CAPConnectionNotifier extends Connection {

	   /**
	    * Waits for a client to connect to this L2CAP service.
	    * Upon connection returns an <code>L2CAPConnection</code>
	    * that can be used to communicate with this client.
	    *
	    * <P> A service record associated with this connection will be
	    * added to the SDDB associated with this
	    * <code>L2CAPConnectionNotifier</code> object if one does not
	    * exist in the SDDB.  This method will put the
	    * local device in connectable mode so that it may respond to
	    * connection attempts by clients.
	    *
	    * <P> The following checks are done to verify that any
	    * modifications made by the application to the service record
	    * after it was created by <code>Connector.open()</code> have not
	    * created an invalid service record.  If any of these checks
	    * fail, then a <code>ServiceRegistrationException</code> is thrown.
	    * <UL>
	    * <LI>ServiceClassIDList and ProtocolDescriptorList, the mandatory
	    * service attributes for a <code>btl2cap</code> service record,
	    * must be present in the service record.
	    * <LI>L2CAP must be in the ProtocolDescriptorList.
	    * <LI>The PSM value must not have changed in the service record.
	    * </UL>
	    * <P>
	    * This method will not ensure that the service record created
	    * is a completely valid service record. It is the responsibility
	    * of the application to ensure that the service record follows
	    * all of the applicable syntactic and semantic rules for service
	    * record correctness.
	    *
	    * @return a connection to communicate with the client
	    *
	    * @exception IOException if the notifier is closed before
	    * <code>acceptAndOpen()</code> is called
	    *
	    * @exception ServiceRegistrationException if the structure of the
	    * associated service record is invalid or if the service record
	    * could not be added successfully to the local SDDB.  The
	    * structure of service record is invalid if the service
	    * record is missing any mandatory service attributes, or has
	    * changed any of the values described above which are fixed and
	    * cannot be changed. Failures to add the record to the SDDB could
	    * be due to insufficient disk space, database locks, etc.
	    *
	    * @exception BluetoothStateException if the server device could
	    * not be placed in connectable mode because the device user has
	    * configured the device to be non-connectable.
	    *
	    */
	    public L2CAPConnection  acceptAndOpen() throws IOException;
	    
}
