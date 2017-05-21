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

/**
 * The <code>HeaderSet</code> interface defines the methods that set and get
 * the values of OBEX headers.
 * <P>
 * The following table describes how the headers specified in this interface are
 * represented in OBEX and in Java. The Java types are used with the
 * <code>setHeader()</code> and <code>getHeader()</code> methods and specify
 * the type of object that must be provided and will be returned from these
 * methods, respectively. <TABLE BORDER>
 * <TR>
 * <TH>Header Values</TH>
 * <TH>OBEX Representation</TH>
 * <TH>Java Type</TH>
 * </TR>
 * <TR>
 * <TD>COUNT</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.lang.Long</code> in the range 0 to 2<sup>32</sup>-1</TD>
 * </TR>
 * <TR>
 * <TD>NAME</TD>
 * <TD>Unicode string</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>TYPE</TD>
 * <TD>ASCII string</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>LENGTH</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.lang.Long</code> in the range 0 to 2<sup>32</sup>-1</TD>
 * </TR>
 * <TR>
 * <TD>TIME_ISO_8601</TD>
 * <TD>ASCII string of the form YYYYMMDDTHHMMSS[Z] where [Z] specifies Zulu
 * time</TD>
 * <TD><code>java.util.Calendar</code></TD>
 * </TR>
 * <TR>
 * <TD>TIME_4_BYTE</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.util.Calendar</code></TD>
 * </TR>
 * <TR>
 * <TD>DESCRIPTION</TD>
 * <TD>Unicode string</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>TARGET</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>HTTP</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>WHO</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>OBJECT_CLASS</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>APPLICATION_PARAMETER</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * </TABLE>
 * <P>
 * The <code>APPLICATION_PARAMETER</code> header requires some additional
 * explanation. The byte array provided with the
 * <code>APPLICATION_PARAMETER</code> should be of the form Tag-Length-Value
 * according to the OBEX specification where Tag is a byte long, Length is a
 * byte long, and Value is up to 255 bytes long. Multiple Tag-Length-Value
 * triples are allowed within a single <code>APPLICATION_PARAMETER</code>
 * header. The implementation will NOT check this condition. It is mentioned
 * only to allow for interoperability between OBEX implementations.
 * <P>
 * <STRONG>User Defined Headers</STRONG>
 * <P>
 * OBEX allows 64 user-defined header values. Depending on the header identifier
 * provided, headers have different types. The table below defines the ranges
 * and their types. <TABLE BORDER>
 * <TR>
 * <TH>Header Identifier</TH>
 * <TH>Decimal Range</TH>
 * <TH>OBEX Type</TH>
 * <TH>Java Type</TH>
 * </TR>
 * <TR>
 * <TD>0x30 to 0x3F</TD>
 * <TD>48 to 63</TD>
 * <TD>Unicode String</TD>
 * <TD><code>java.lang.String</code></TD>
 * </TR>
 * <TR>
 * <TD>0x70 to 0x7F</TD>
 * <TD>112 to 127</TD>
 * <TD>byte sequence</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 * <TR>
 * <TD>0xB0 to 0xBF</TD>
 * <TD>176 to 191</TD>
 * <TD>1 byte</TD>
 * <TD><code>java.lang.Byte</code></TD>
 * </TR>
 * <TR>
 * <TD>0xF0 to 0xFF</TD>
 * <TD>240 to 255</TD>
 * <TD>4 byte unsigned integer</TD>
 * <TD><code>java.lang.Long</code> in the range 0 to 2<sup>32</sup>-1</TD>
 * </TR>
 * </TABLE>
 * 
 * @version 1.0 February 11, 2002
 */
public interface HeaderSet {

	/**
	 * Represents the OBEX Count header. This allows the connection statement to
	 * tell the server how many objects it plans to send or retrieve.
	 * <P>
	 * The value of <code>COUNT</code> is 0xC0 (192).
	 */
	public static final int COUNT = 0xC0;

	/**
	 * Represents the OBEX Name header. This specifies the name of the object.
	 * <P>
	 * The value of <code>NAME</code> is 0x01 (1).
	 */
	public static final int NAME = 0x01;

	/**
	 * Represents the OBEX Type header. This allows a request to specify the
	 * type of the object (e.g. text, html, binary, etc.).
	 * <P>
	 * The value of <code>TYPE</code> is 0x42 (66).
	 */
	public static final int TYPE = 0x42;

	/**
	 * Represents the OBEX Length header. This is the length of the object in
	 * bytes.
	 * <P>
	 * The value of <code>LENGTH</code> is 0xC3 (195).
	 */
	public static final int LENGTH = 0xC3;

	/**
	 * Represents the OBEX Time header using the ISO 8601 standards. This is the
	 * preferred time header.
	 * <P>
	 * The value of <code>TIME_ISO_8601</code> is 0x44 (68).
	 */
	public static final int TIME_ISO_8601 = 0x44;

	/**
	 * Represents the OBEX Time header using the 4 byte representation. This is
	 * only included for backwards compatibility. It represents the number of
	 * seconds since January 1, 1970.
	 * <P>
	 * The value of <code>TIME_4_BYTE</code> is 0xC4 (196).
	 */
	public static final int TIME_4_BYTE = 0xC4;

	/**
	 * Represents the OBEX Description header. This is a text description of the
	 * object.
	 * <P>
	 * The value of <code>DESCRIPTION</code> is 0x05 (5).
	 */
	public static final int DESCRIPTION = 0x05;

	/**
	 * Represents the OBEX Target header. This is the name of the service an
	 * operation is targeted to.
	 * <P>
	 * The value of <code>TARGET</code> is 0x46 (70).
	 */
	public static final int TARGET = 0x46;

	/**
	 * Represents the OBEX HTTP header. This allows an HTTP 1.X header to be
	 * included in a request or reply.
	 * <P>
	 * The value of <code>HTTP</code> is 0x47 (71).
	 */
	public static final int HTTP = 0x47;

	/**
	 * Represents the OBEX Who header. Identifies the OBEX application to
	 * determine if the two peers are talking to each other.
	 * <P>
	 * The value of <code>WHO</code> is 0x4A (74).
	 */
	public static final int WHO = 0x4A;

	/**
	 * Represents the OBEX Object Class header. This header specifies the OBEX
	 * object class of the object.
	 * <P>
	 * The value of <code>OBJECT_CLASS</code> is 0x4F (79).
	 */
	public static final int OBJECT_CLASS = 0x4F;

	/**
	 * Represents the OBEX Application Parameter header. This header specifies
	 * additional application request and response information.
	 * <P>
	 * The value of <code>APPLICATION_PARAMETER</code> is 0x4C (76).
	 */
	public static final int APPLICATION_PARAMETER = 0x4C;

	/**
	 * Sets the value of the header identifier to the value provided. The type
	 * of object must correspond to the Java type defined in the description of
	 * this interface. If <code>null</code> is passed as the
	 * <code>headerValue</code> then the header will be removed from the set
	 * of headers to include in the next request.
	 * 
	 * @param headerID
	 *            the identifier to include in the message
	 * 
	 * @param headerValue
	 *            the value of the header identifier
	 * 
	 * @exception IllegalArgumentException
	 *                if the header identifier provided is not one defined in
	 *                this interface or a user-defined header; if the type of
	 *                <code>headerValue</code> is not the correct Java type as
	 *                defined in the description of this interface
	 */
	public void setHeader(int headerID, Object headerValue);

	/**
	 * Retrieves the value of the header identifier provided. The type of the
	 * Object returned is defined in the description of this interface.
	 * 
	 * @param headerID
	 *            the header identifier whose value is to be returned
	 * 
	 * @return the value of the header provided or <code>null</code> if the
	 *         header identifier specified is not part of this
	 *         <code>HeaderSet</code> object
	 * 
	 * @exception IllegalArgumentException
	 *                if the <code>headerID</code> is not one defined in this
	 *                interface or any of the user-defined headers
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer during the
	 *                operation or if the connection has been closed
	 */
	public Object getHeader(int headerID) throws IOException;

	/**
	 * Retrieves the list of headers that may be retrieved via the
	 * <code>getHeader</code> method that will not return <code>null</code>.
	 * In other words, this method returns all the headers that are available in
	 * this object.
	 * 
	 * @see #getHeader
	 * 
	 * @return the array of headers that are set in this object or
	 *         <code>null</code> if no headers are available
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer during the
	 *                operation or the connection has been closed
	 */
	public int[] getHeaderList() throws IOException;

	/**
	 * Sets the authentication challenge header. The <code>realm</code> will
	 * be encoded based upon the default encoding scheme used by the
	 * implementation to encode strings. Therefore, the encoding scheme used to
	 * encode the <code>realm</code> is application dependent.
	 * 
	 * @param realm
	 *            a short description that describes what password to use; if
	 *            <code>null</code> no realm will be sent in the
	 *            authentication challenge header
	 * 
	 * @param userID
	 *            if <code>true</code>, a user ID is required in the reply;
	 *            if <code>false</code>, no user ID is required
	 * 
	 * @param access
	 *            if <code>true</code> then full access will be granted if
	 *            successful; if <code>false</code> then read-only access will
	 *            be granted if successful
	 */
	public void createAuthenticationChallenge(String realm, boolean userID, boolean access);

	/**
	 * Returns the response code received from the server. Response codes are
	 * defined in the <code>ResponseCodes</code> class.
	 * 
	 * @see ResponseCodes
	 * 
	 * @return the response code retrieved from the server
	 * 
	 * @exception IOException
	 *                if an error occurred in the transport layer during the
	 *                transaction; if this method is called on a
	 *                <code>HeaderSet</code> object created by calling
	 *                <code>createHeaderSet()</code> in a
	 *                <code>ClientSession</code> object; if an OBEX server
	 *                created this object
	 */
	public int getResponseCode() throws IOException;
}
