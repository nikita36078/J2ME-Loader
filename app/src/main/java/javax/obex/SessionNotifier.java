/**
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *
 *
 *  @version $Id$
 */ 
package javax.obex;

import java.io.IOException;

import javax.microedition.io.Connection;

/**
 * The <code>SessionNotifier</code> interface defines a connection notifier
 * for server-side OBEX connections. When a <code>SessionNotifier</code> is
 * created and calls <code>acceptAndOpen()</code>, it will begin listening
 * for clients to create a connection at the transport layer. When the transport
 * layer connection is received, the <code>acceptAndOpen()</code> method will
 * return a <code>javax.microedition.io.Connection</code> that is the
 * connection to the client. The <code>acceptAndOpen()</code> method also
 * takes a <code>ServerRequestHandler</code> argument that will process the
 * requests from the client that connects to the server.
 * 
 * @version 1.0 February 11, 2002
 */
public interface SessionNotifier extends Connection {

	/**
	 * Waits for a transport layer connection to be established and specifies
	 * the handler to handle the requests from the client. No authenticator is
	 * associated with this connection, therefore, it is implementation
	 * dependent as to how an authentication challenge and authentication
	 * response header will be received and processed.
	 * <P>
	 * <H4>Additional Note for OBEX over Bluetooth</H4>
	 * If this method is called on a <code>SessionNotifier</code> object that
	 * does not have a <code>ServiceRecord</code> in the SDDB, the
	 * <code>ServiceRecord</code> for this object will be added to the SDDB.
	 * This method requests the BCC to put the local device in connectable mode
	 * so that it will respond to connection attempts by clients.
	 * <P>
	 * The following checks are done to verify that the service record provided
	 * is valid. If any of these checks fail, then a
	 * <code>ServiceRegistrationException</code> is thrown.
	 * <UL>
	 * <LI>ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a <code>btgoep</code> service record, must be present in
	 * the <code>ServiceRecord</code> associated with this notifier.
	 * <LI>L2CAP, RFCOMM and OBEX must all be in the ProtocolDescriptorList
	 * <LI>The <code>ServiceRecord</code> associated with this notifier must
	 * not have changed the RFCOMM server channel number
	 * </UL>
	 * <P>
	 * This method will not ensure that <code>ServiceRecord</code> associated
	 * with this notifier is a completely valid service record. It is the
	 * responsibility of the application to ensure that the service record
	 * follows all of the applicable syntactic and semantic rules for service
	 * record correctness.
	 * <p>
	 * Note : once an application invokes {@code close()} on any
	 * {@code L2CAPConnectionNotifier}, {@code SessionNotifier}, or
	 * {@code StreamConnectionNotifer} instance, all pending
	 * {@code acceptAndOpen()} methods that have been invoked previously on that
	 * instance MUST throw {@code InterruptedIOException}. This mechanism
	 * provides an application with the means to cancel any outstanding
	 * {@code acceptAndOpen()} method calls
	 * 
	 * @param handler
	 *            the request handler that will respond to OBEX requests
	 * 
	 * @return the connection to the client
	 * 
	 * @exception IOException
	 *                if an error occurs in the transport layer
	 * 
	 * @exception NullPointerException
	 *                if <code>handler</code> is <code>null</code>
	 * 
	 * @exception ServiceRegistrationException
	 *                if the structure of the associated service record is
	 *                invalid or if the service record could not be added
	 *                successfully to the local SDDB. The structure of service
	 *                record is invalid if the service record is missing any
	 *                mandatory service attributes, or has changed any of the
	 *                values described above which are fixed and cannot be
	 *                changed. Failures to add the record to the SDDB could be
	 *                due to insufficient disk space, database locks, etc.
	 * 
	 * @exception BluetoothStateException
	 *                if the server device could not be placed in connectable
	 *                mode because the device user has configured the device to
	 *                be non-connectable
	 */
	public Connection acceptAndOpen(ServerRequestHandler handler) throws IOException;

	/**
	 * Waits for a transport layer connection to be established and specifies
	 * the handler to handle the requests from the client and the
	 * <code>Authenticator</code> to use to respond to authentication
	 * challenge and authentication response headers.
	 * <P>
	 * <H4>Additional Note for OBEX over Bluetooth</H4>
	 * If this method is called on a <code>SessionNotifier</code> object that
	 * does not have a <code>ServiceRecord</code> in the SDDB, the
	 * <code>ServiceRecord</code> for this object will be added to the SDDB.
	 * This method requests the BCC to put the local device in connectable mode
	 * so that it will respond to connection attempts by clients.
	 * <P>
	 * The following checks are done to verify that the service record provided
	 * is valid. If any of these checks fail, then a
	 * <code>ServiceRegistrationException</code> is thrown.
	 * <UL>
	 * <LI>ServiceClassIDList and ProtocolDescriptorList, the mandatory service
	 * attributes for a <code>btgoep</code> service record, must be present in
	 * the <code>ServiceRecord</code> associated with this notifier.
	 * <LI>L2CAP, RFCOMM and OBEX must all be in the ProtocolDescriptorList
	 * <LI>The <code>ServiceRecord</code> associated with this notifier must
	 * not have changed the RFCOMM server channel number
	 * </UL>
	 * <P>
	 * This method will not ensure that <code>ServiceRecord</code> associated
	 * with this notifier is a completely valid service record. It is the
	 * responsibility of the application to ensure that the service record
	 * follows all of the applicable syntactic and semantic rules for service
	 * record correctness.
	 * <p>
	 * Note : once an application invokes {@code close()} on any
	 * {@code L2CAPConnectionNotifier}, {@code SessionNotifier}, or
	 * {@code StreamConnectionNotifer} instance, all pending
	 * {@code acceptAndOpen()} methods that have been invoked previously on that
	 * instance MUST throw {@code InterruptedIOException}. This mechanism
	 * provides an application with the means to cancel any outstanding
	 * {@code acceptAndOpen()} method calls
	 * 
	 * @param handler
	 *            the request handler that will respond to OBEX requests
	 * 
	 * @param auth
	 *            the <code>Authenticator</code> to use with this connection;
	 *            if <code>null</code> then no <code>Authenticator</code>
	 *            will be used
	 * 
	 * @return the connection to the client
	 * 
	 * @exception IOException
	 *                if an error occurs in the transport layer
	 * 
	 * @exception NullPointerException
	 *                if <code>handler</code> is <code>null</code>
	 * 
	 * @exception ServiceRegistrationException
	 *                if the structure of the associated service record is
	 *                invalid or if the service record could not be added
	 *                successfully to the local SDDB. The structure of service
	 *                record is invalid if the service record is missing any
	 *                mandatory service attributes, or has changed any of the
	 *                values described above which are fixed and cannot be
	 *                changed. Failures to add the record to the SDDB could be
	 *                due to insufficient disk space, database locks, etc.
	 * 
	 * @exception BluetoothStateException
	 *                if the server device could not be placed in connectable
	 *                mode because the device user has configured the device to
	 *                be non-connectable
	 */
	public Connection acceptAndOpen(ServerRequestHandler handler, Authenticator auth) throws IOException;
}
