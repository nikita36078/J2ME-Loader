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
 * The <code>ClientSession</code> interface provides methods for OBEX
 * requests. This interface provides a way to define headers for any OBEX
 * operation. OBEX operations are CONNECT, SETPATH, PUT, GET and DISCONNECT. For
 * PUTs and GETs, this interface will return a <code>javax.obex.Operation</code>
 * object to complete the operations. For CONNECT, DISCONNECT, and SETPATH
 * operations, this interface will complete the operation and return the result
 * in a <code>HeaderSet</code> object.
 * <P>
 * <STRONG>Connection ID and Target Headers</STRONG>
 * <P>
 * According to the IrOBEX specification, a packet may not contain a Connection
 * ID and Target header. Since the Connection ID header is managed by the
 * implementation, it will not send a Connection ID header if a Connection ID
 * was specified in a packet that has a Target header. In other words, if an
 * application adds a Target header to a <code>HeaderSet</code> object used in
 * an OBEX operation and a Connection ID was specified, no Connection ID will be
 * sent in the packet containing the Target header.
 * <P>
 * <STRONG>CREATE-EMPTY and PUT-DELETE Requests</STRONG>
 * <P>
 * To perform a CREATE-EMPTY request, the client must call the
 * <code>put()</code> method. With the <code>Operation</code> object
 * returned, the client must open the output stream by calling
 * <code>openOutputStream()</code> and then close the stream by calling
 * <code>close()</code> on the <code>OutputStream</code> without writing any
 * data. Using the <code>DataOutputStream</code> returned from
 * <code>openDataOutputStream()</code> works the same way.
 * <P>
 * There are two ways to perform a PUT-DELETE request. The <code>delete()</code>
 * method is one way to perform a PUT-DELETE request. The second way to perform
 * a PUT-DELETE request is by calling <code>put()</code> and never calling
 * <code>openOutputStream()</code> or <code>openDataOutputStream()</code> on
 * the <code>Operation</code> object returned from <code>put()</code>.
 * <P>
 * <STRONG>PUT example</STRONG>
 * <P>
 * 
 * <pre>
 * void putObjectViaOBEX(ClientSession conn, HeaderSet head, byte[] obj) throws IOException {
 * 
 * 	// Include the length header
 * 	head.setHeader(HeaderSet.LENGTH, new Long(obj.length));
 * 
 * 	// Initiate the PUT request
 * 	Operation op = conn.put(head);
 * 
 * 	// Open the output stream to put the object to it
 * 	OutputStream out = op.openOutputStream();
 * 
 * 	// Send the object to the server
 * 	out.write(obj);
 * 
 * 	// End the transaction
 * 	out.close();
 * 	op.close();
 * }
 * </pre>
 * 
 * <P>
 * <STRONG>GET example</STRONG>
 * <P>
 * 
 * <pre>
 * byte[] getObjectViaOBEX(ClientSession conn, HeaderSet head) throws IOException {
 * 
 * 	// Send the initial GET request to the server
 * 	Operation op = conn.get(head);
 * 
 * 	// Get the object from the input stream
 * 	InputStream in = op.openInputStream();
 * 
 * 	ByteArrayOutputStream out = new ByteArrayOutputStream();
 * 	int data = in.read();
 * 	while (data != -1) {
 * 		out.write((byte) data);
 * 		data = in.read();
 * 	}
 * 
 * 	// End the transaction
 * 	in.close();
 * 	op.close();
 * 
 * 	byte[] obj = out.toByteArray();
 * 	out.close();
 * 
 * 	return obj;
 * }
 * </pre>
 * 
 * @version 1.0 February 11, 2002
 */
public interface ClientSession extends Connection {

	/**
	 * Sets the <code>Authenticator</code> to use with this connection. The
	 * <code>Authenticator</code> allows an application to respond to
	 * authentication challenge and authentication response headers. If no
	 * <code>Authenticator</code> is set, the response to an authentication
	 * challenge or authentication response header is implementation dependent.
	 * 
	 * @param auth
	 *            the <code>Authenticator</code> to use for this connection
	 * 
	 * @exception NullPointerException
	 *                if <code>auth</code> is <code>null</code>
	 */
	public void setAuthenticator(Authenticator auth);

	/**
	 * Creates a <code>javax.obex.HeaderSet</code> object. This object can be
	 * used to define header values in a request.
	 * 
	 * @see HeaderSet
	 * 
	 * @return a new <code>javax.obex.HeaderSet</code> object
	 */
	public HeaderSet createHeaderSet();

	/**
	 * Sets the connection ID header to include in the request packets. If a
	 * connection ID is set, it will be sent in each request to the server
	 * except for the CONNECT request. An application only needs to set the
	 * connection ID if it is trying to operate with different targets over the
	 * same transport layer connection. If a client receives a connection ID
	 * from the server, the implementation will continue to use that connection
	 * ID until the application changes it or until the connection is closed.
	 * 
	 * @param id
	 *            the connection ID to use
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>id</code> is not in the range 0 to 2<sup>32</sup>-1
	 */
	public void setConnectionID(long id);

	/**
	 * Retrieves the connection ID that is being used in the present connection.
	 * This method will return -1 if no connection ID is being used.
	 * 
	 * @return the connection ID being used or -1 if no connection ID is being
	 *         used
	 */
	public long getConnectionID();

	/**
	 * Completes an OBEX CONNECT operation. If the <code>headers</code>
	 * argument is <code>null</code>, no headers will be sent in the request.
	 * This method will never return <code>null</code>.
	 * <P>
	 * This method must be called and a successful response code of
	 * <code>OBEX_HTTP_OK</code> must be received before <code>put()</code>,
	 * <code>get()</code>, <code>setPath()</code>, <code>delete()</code>,
	 * or <code>disconnect()</code> may be called. Similarly, after a
	 * successful call to <code>disconnect()</code>, this method must be
	 * called before calling <code>put()</code>, <code>get()</code>,
	 * <code>setPath()</code>, <code>delete()</code>, or
	 * <code>disconnect()</code>.
	 * 
	 * @param headers
	 *            the headers to send in the CONNECT request
	 * 
	 * @return the headers that were returned from the server
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer; if the client
	 *                is already in an operation; if this method had already
	 *                been called with a successful response code of
	 *                <code>OBEX_HTTP_OK</code> and calls to
	 *                <code>disconnect()</code> have not returned a response
	 *                code of <code>OBEX_HTTP_OK</code>; if the headers
	 *                defined in <code>headers</code> exceed the max packet
	 *                length
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>headers</code> was not created by a call to
	 *                <code>createHeaderSet()</code>
	 */
	public HeaderSet connect(HeaderSet headers) throws IOException;

	/**
	 * Completes an OBEX DISCONNECT operation. If the <code>headers</code>
	 * argument is <code>null</code>, no headers will be sent in the request.
	 * This method will end the session. A new session may be started by calling
	 * <code>connect()</code>. This method will never return
	 * <code>null</code>.
	 * 
	 * @param headers
	 *            the header to send in the DISCONNECT request
	 * 
	 * @return the headers returned by the server
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer; if the client
	 *                is already in an operation; if an OBEX connection does not
	 *                exist because <code>connect()</code> has not been
	 *                called; if <code>disconnect()</code> has been called and
	 *                received a response code of <code>OBEX_HTTP_OK</code>
	 *                after the last call to <code>connect()</code>; if the
	 *                headers defined in <code>headers</code> exceed the max
	 *                packet length
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>headers</code> were not created by a call to
	 *                <code>createHeaderSet()</code>
	 */
	public HeaderSet disconnect(HeaderSet headers) throws IOException;

	/**
	 * Completes an OBEX SETPATH operation. This method will never return
	 * <code>null</code>.
	 * 
	 * @param backup
	 *            if <code>true</code>, instructs the server to back up one
	 *            directory before moving to the directory specified in name
	 *            (similar to cd .. on PCs); if <code>false</code>, apply
	 *            <code>name</code> to the current directory
	 * 
	 * @param create
	 *            if <code>true</code>, instructs the server to create the
	 *            directory if it does not exist; if <code>false</code>,
	 *            instruct the server to return an error code if the directory
	 *            does not exist
	 * 
	 * @param headers
	 *            the headers to include in the SETPATH request
	 * 
	 * @return the headers that were returned from the server
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer; if the client
	 *                is already in an operation; if an OBEX connection does not
	 *                exist because <code>connect()</code> has not been
	 *                called; if <code>disconnect()</code> had been called and
	 *                a response code of <code>OBEX_HTTP_OK</code> was
	 *                received; if the headers defined in <code>headers</code>
	 *                exceed the max packet length
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>headers</code> were not created by a call to
	 *                <code>createHeaderSet()</code>
	 */
	public HeaderSet setPath(HeaderSet headers, boolean backup, boolean create) throws IOException;

	/**
	 * Performs an OBEX DELETE operation. This method will never return
	 * <code>null</code>.
	 * 
	 * @param headers
	 *            the header to send in the DELETE request
	 * 
	 * @return the headers returned by the server
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer; if the client
	 *                is already in an operation; if an OBEX connection does not
	 *                exist because <code>connect()</code> has not been
	 *                called; if <code>disconnect()</code> had been called and
	 *                a response code of <code>OBEX_HTTP_OK</code> was
	 *                received; if the headers defined in <code>headers</code>
	 *                exceed the max packet length
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>headers</code> were not created by a call to
	 *                <code>createHeaderSet()</code>
	 */
	public HeaderSet delete(HeaderSet headers) throws IOException;

	/**
	 * Performs an OBEX GET operation. This method will send the OBEX headers
	 * provided to the server and return an <code>Operation</code> object to
	 * continue with the operation. This method will never return
	 * <code>null</code>.
	 * 
	 * @see Operation
	 * 
	 * @param headers
	 *            the OBEX headers to send as part of the initial GET request
	 * 
	 * @return the OBEX operation that will complete the GET request
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer; if an OBEX
	 *                connection does not exist because <code>connect()</code>
	 *                has not been called; if <code>disconnect()</code> had
	 *                been called and a response code of
	 *                <code>OBEX_HTTP_OK</code> was received; if
	 *                <code>connect()</code> has not been called; if the
	 *                client is already in an operation;
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>headers</code> were not created by a call to
	 *                <code>createHeaderSet()</code>
	 */
	public Operation get(HeaderSet headers) throws IOException;

	/**
	 * Performs an OBEX PUT operation. This method will send the OBEX headers
	 * provided to the server and return an <code>Operation</code> object to
	 * continue with the PUT operation. This method will never return
	 * <code>null</code>.
	 * 
	 * @see Operation
	 * 
	 * @param headers
	 *            the OBEX headers to send in the initial PUT request
	 * 
	 * @return the operation object used to complete the PUT request
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer; if an OBEX
	 *                connection does not exist because <code>connect()</code>
	 *                has not been called; if <code>disconnect()</code> had
	 *                been called and a response code of
	 *                <code>OBEX_HTTP_OK</code> was received; if
	 *                <code>connect()</code> has not been called; if the
	 *                client is already in an operation;
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>headers</code> were not created by a call to
	 *                <code>createHeaderSet()</code>
	 */
	public Operation put(HeaderSet headers) throws IOException;
}
