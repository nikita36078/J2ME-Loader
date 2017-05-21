/**
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *
 *
 *  @version $Id$
 */ 

package javax.bluetooth;

/**
 * The <code>DiscoveryListener</code> interface allows an application to
 * receive device discovery and service discovery events. This interface
 * provides four methods, two for discovering devices and two for discovering
 * services.
 * 
 * @version 1.0 February 11, 2002
 * 
 * @since 1.1 The JSR 82 specification does not require that implementations
 *        create individual threads for event delivery. Thus, if a
 *        DiscoveryListener method does not return or the return is delayed, the
 *        system may be blocked. So the following note is given for application
 *        developers :
 *        <p>
 * 
 * The following DiscoveryListener methods SHOULD return immediately :
 * <ul>
 * <li>DiscoveryListener.deviceDiscovered</li>
 * <li>DiscoveryListener.inquiryCompleted</li>
 * <li>DiscoveryListener.servicesDiscovered</li>
 * <li>DiscoveryListener.serviceSearchCompleted</li>
 * </ul>
 * 
 */
public interface DiscoveryListener {

	/**
	 * Indicates the normal completion of device discovery. Used with the
	 * {@link #inquiryCompleted(int)} method. 
	 * <p>
	 * The value of INQUIRY_COMPLETED is 0x00 (0).
	 * 
	 * @see #inquiryCompleted(int)
	 * @see DiscoveryAgent#startInquiry(int, javax.bluetooth.DiscoveryListener)
	 */
	public static final int INQUIRY_COMPLETED = 0x00;

	/**
	 * Indicates device discovery has been canceled by the application and did
	 * not complete. Used with the {@link #inquiryCompleted(int)} method. 
	 * <p>
	 * The value of INQUIRY_TERMINATED is 0x05 (5).
	 * 
	 * @see  #inquiryCompleted(int)
	 * @see DiscoveryAgent#startInquiry(int, javax.bluetooth.DiscoveryListener)
	 * @see DiscoveryAgent#cancelInquiry(javax.bluetooth.DiscoveryListener)
	 */
	public static final int INQUIRY_TERMINATED = 0x05;

	/**
	 * Indicates that the inquiry request failed to complete normally, but was
	 * not cancelled. 
	 * <p>
	 * The value of INQUIRY_ERROR is 0x07 (7).
	 * 
	 * @see  #inquiryCompleted(int)
	 * @see DiscoveryAgent#startInquiry(int, javax.bluetooth.DiscoveryListener)
	 */
	public static final int INQUIRY_ERROR = 0x07;

	/**
	 * Indicates the normal completion of service discovery. Used with the
	 * {@link #serviceSearchCompleted(int, int)} method. 
	 * <p>
	 * The value of SERVICE_SEARCH_COMPLETED is 0x01 (1).
	 * 
	 * @see #serviceSearchCompleted(int, int)
	 * @see DiscoveryAgent#searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */
	public static final int SERVICE_SEARCH_COMPLETED = 0x01;

	/**
	 * Indicates the service search has been canceled by the application and did
	 * not complete. Used with the {@link #serviceSearchCompleted(int, int)} method. 
	 * <p>
	 * The value of SERVICE_SEARCH_TERMINATED is 0x02 (2).
	 * 
	 * @see  #serviceSearchCompleted(int, int)
	 * @see DiscoveryAgent#searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 * @see DiscoveryAgent#cancelServiceSearch(int)
	 */
	public static final int SERVICE_SEARCH_TERMINATED = 0x02;

	/**
	 * Indicates the service search terminated with an error. Used with the
	 * {@link #serviceSearchCompleted(int, int)} method. 
	 * <p>
	 * The value of SERVICE_SEARCH_ERROR is 0x03 (3).
	 * 
	 * @see #serviceSearchCompleted(int, int)
	 * @see DiscoveryAgent#searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */
	public static final int SERVICE_SEARCH_ERROR = 0x03;

	/**
	 * Indicates the service search has completed with no service records found
	 * on the device. Used with the {@link #serviceSearchCompleted(int, int)} method. 
	 * <p>
	 * The value of SERVICE_SEARCH_NO_RECORDS is 0x04 (4).
	 * 
	 * @see #serviceSearchCompleted(int, int)
	 * @see DiscoveryAgent#searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */
	public static final int SERVICE_SEARCH_NO_RECORDS = 0x04;

	/**
	 * Indicates the service search could not be completed because the remote
	 * device provided to {@link DiscoveryAgent#searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener) 
	 * DiscoveryAgent.searchServices()} could not be reached.
	 * Used with the {@link #serviceSearchCompleted(int, int)} method. 
	 * <P>
	 * The value of SERVICE_SEARCH_DEVICE_NOT_REACHABLE is 0x06 (6).
	 * 
	 * @see #serviceSearchCompleted(int, int)
	 * @see DiscoveryAgent#searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */
	public static final int SERVICE_SEARCH_DEVICE_NOT_REACHABLE = 0x06;

	/**
	 * Called when a device is found during an inquiry. An inquiry searches for
	 * devices that are discoverable. The same device may be returned multiple
	 * times. 
	 * 
	 * @param btDevice the device that was found during the inquiry 
	 * @param cod - the service classes, major device class, and minor device
	 * class of the remote device 
	 * @see DiscoveryAgent#startInquiry(int, javax.bluetooth.DiscoveryListener)
	 */
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod);

	/**
	 * Called when service(s) are found during a service search. 
	 * 
	 * @param transID the transaction ID of the service search that is posting the
	 * result 
	 * @param servRecord  a list of services found during the search request 
	 * @see DiscoveryAgent#searchServices(int[], javax.bluetooth.UUID[],
	 * javax.bluetooth.RemoteDevice, javax.bluetooth.DiscoveryListener)
	 */
	public void servicesDiscovered(int transID, ServiceRecord[] servRecord);

	/**
	 * Called when a service search is completed or was terminated because of an
	 * error. Legal status values in the {@code respCode} argument include
	 * {@link #SERVICE_SEARCH_COMPLETED}, {@link #SERVICE_SEARCH_TERMINATED},
	 * {@link #SERVICE_SEARCH_ERROR}, {@link #SERVICE_SEARCH_NO_RECORDS} and
	 * {@link #SERVICE_SEARCH_DEVICE_NOT_REACHABLE}. The following table describes when
	 * each {@code respCode} will be used: 
	 * <table><tr><th>respCode</th><th>Reason</th></tr>
	 * <tr><td>{@link #SERVICE_SEARCH_COMPLETED}</td>
	 * 			<td>if the service search completed normally</td></tr>
	 * <tr><td>{@link #SERVICE_SEARCH_TERMINATED}</td>
	 * 			<td>if the service search request was cancelled by a call to
	 * 			{@link DiscoveryAgent#cancelServiceSearch(int)}</td></tr>
	 * <tr><td>{@link #SERVICE_SEARCH_ERROR}</td>
	 * 			<td>if an error occurred while processing the request</td></tr>
	 * <tr><td>{@link #SERVICE_SEARCH_NO_RECORDS}</td>
	 * 			<td>if no records were found during the service search</td></tr>
	 * <tr><td>{@link #SERVICE_SEARCH_DEVICE_NOT_REACHABLE}</td>
	 * 			<td>if the device specified in the search request could not be reached or 
	 * 			the local device could not establish a connection to the remote device
	 * </td></tr></table>
	 * 
	 * @param transID the transaction ID identifying the request which
	 * initiated the service search 
	 * @param respCode  the response code that indicates the status of the transaction
	 */
	public void serviceSearchCompleted(int transID, int respCode);

	/**
	 * Called when an inquiry is completed. The {@code discType} will be
	 * {@link #INQUIRY_COMPLETED} if the inquiry ended normally or {@link #INQUIRY_TERMINATED}
	 * if the inquiry was canceled by a call to 
	 * {@link DiscoveryAgent#cancelInquiry(DiscoveryListener)}. The {@code discType} will be 
	 * {@link #INQUIRY_ERROR} if an error occurred while processing the inquiry causing the 
	 * inquiry to end abnormally. 
	 * 
	 * @param discType the type of request that was completed; either 
	 * 				{@link #INQUIRY_COMPLETED}, {@link #INQUIRY_TERMINATED}, 
	 * 				or {@link #INQUIRY_ERROR}
	 * @see #INQUIRY_COMPLETED
	 * @see #INQUIRY_TERMINATED
	 * @see #INQUIRY_ERROR
	 */
	public void inquiryCompleted(int discType);
}
