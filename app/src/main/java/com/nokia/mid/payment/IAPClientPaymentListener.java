package com.nokia.mid.payment;

/**
 * The <code>IAPClientPaymentListener</code> represents a listener that receives
 * events associated with the <code>IAPClientPaymentProvider</code>. Applications implement this interface and
 * register it with the <code>IAPClientPaymentProvider</code> to obtain asynchronous callback responses to the API requests.
 * <p>
 * The status parameter being returned in the callback functions below can have one of the values defined in the
 * final fields of the interface.
 */
public interface IAPClientPaymentListener {

	/**
	 * The status is OK which indicates no errors.
	 */
	int OK = 1;

	/**
	 * Http error status.
	 */
	int BAD_REQUEST = -1;

	/**
	 * Http error status.
	 */
	int AUTH_FAILED = -2;

	/**
	 * Http error status.
	 */
	int FORBIDDEN = -3;

	/**
	 * Http error status.
	 */
	int NOT_FOUND = -4;

	/**
	 * Http error status.
	 */
	int SERVER_ERROR = -5;

	/**
	 * Http error status.
	 */
	int SERVICE_UNAVAILABLE = -6;

	/**
	 * Unknown service.
	 */
	int UNKNOWN_SERVICE = -7;

	/**
	 * Product Id is not valid.
	 */
	int INVALID_PRODUCT_ID = -8;

	/**
	 * Product information failed.
	 */
	int PRODUCT_INFO_FAILED = -9;

	/**
	 * Price of the product is invalid.
	 */
	int INVALID_PRICE = -10;

	/**
	 * Customer information failed.
	 */
	int CUST_INFO_FAILED = -11;

	/**
	 * Payment failed using the stored instrument id.
	 */
	int PMT_INSTR_FAILED = -12;

	/**
	 * No payment methods are set up in the user's Nokia account.
	 */
	int NO_PMT_METHODS = -13;

	/**
	 * Purchase session has failed.
	 */
	int PURCHASE_SESSION_FAILED = -14;

	/**
	 * Unknown product.
	 */
	int UNKNOWN_PRODUCT = -15;

	/**
	 * Invalid Product data.
	 */
	int INVALID_PRODUCT_DATA = -16;

	/**
	 * Delivery limit exceeded.
	 */
	int DELIVERY_LIMIT_EXCEEDED = -17;

	/**
	 * The item is restorable.
	 */
	int RESTORABLE = -18;

	/**
	 * Restoration is not allowed for this product.
	 */
	int RESTORATION_NOT_ALLOWED = -19;

	/**
	 * Number of restorations allowed for this item has exceeded limit.
	 */
	int RESTORATION_LMT_EXCEEDED = -20;

	/**
	 * Number of restorations allowed on the device has exceeded limit.
	 */
	int RESTORATION_DEVICE_LMT_EXCEEDED = -21;

	/**
	 * General Product Error
	 */
	int GENERAL_PRODUCT_ERROR = -22;

	/**
	 * Error in connecting to DRM server.
	 */
	int DRM_SERVER_ERROR = -23;

	/**
	 * Error in activating license.
	 */
	int LICENSE_ACTIVATION_ERROR = -24;

	/**
	 * Silent flag is on the request, but authorization is required.
	 */
	int SILENT_OPER_FAILED = -25;

	/**
	 * User has given wrong credentials 3 times.
	 */
	int OVI_SIGN_IN_FAILED = -26;

	/**
	 * Sending SMS has failed in operator payment.
	 */
	int SMS_PMT_FAILED = -27;

	/**
	 * Operator payment failed.
	 */
	int OPERATOR_BILLING_FAILED = -28;

	/**
	 * Unauthorized payment instrument.
	 */
	int PMT_INSTR_UNAUTHORIZED = -29;

	/**
	 * Unknown transaction id.
	 */
	int UNKNOWN_TRANSACTION_ID = -30;

	/**
	 * Timeout.
	 */
	int TIMEOUT = -31;

	/**
	 * Timeout deliverd.
	 */
	int TIMEOUT_DELIVERED = -32;

	/**
	 * General Purchase Error
	 */
	int GENERAL_PURCHASE_ERROR = -33;

	/**
	 * Operation Failed
	 */
	int OPERATION_FAILED = -34;

	/**
	 * General HTTP Error
	 */
	int GENERAL_HTTP_ERROR = -35;

	/**
	 * General User and Device Info Error
	 */
	int USER_AND_DEVICE_INFO_FAILED = -36;

	/**
	 * General Restore Product Error
	 */
	int RESTORATION_FAILED = -37;

	/**
	 * This callback function is executed when the product data request for the Nokia Store back end
	 * is complete. Product data is returned as a IAPClientProductData class that contains information
	 * about the requested product ID. The product is requested using getProductData().
	 * <p>
	 * The results returned by the IAPClientProductData object will be valid only during the scope of the callback method.
	 * The application will receive unpredictable results if it attempts to use the object at any other time.
	 * <p>
	 *
	 * @param status The status of the getProductData() request.
	 * @param pd     The IAPClientProductData class containing the metadata of the requested product.
	 */
	void productDataReceived(int status, IAPClientProductData pd);

	/**
	 * This callback function is executed when the request for a list of product data to the Nokia Store back end
	 * is complete. The list of product data is returned as a IAPClientProductData array that contains information
	 * about the requested product IDs. The list of product is requested using getProductData().
	 * <p>
	 * The results returned by the IAPClientProductData object array will be valid only during the scope of the callback method.
	 * The application will receive unpredictable results if it attempts to use these objects at any other time.
	 * <p>
	 *
	 * @param status          The status of the getProductData() request.
	 * @param productDataList The IAPClientProductData array containing the metadata of the requested products.
	 */
	void productDataListReceived(int status, IAPClientProductData[] productDataList);

	/**
	 * This callback function is executed when the IAPClient gets information from the Nokia Store
	 * back end indicating that the purchase has been completed.
	 * <p>
	 *
	 * @param status         The status of the purchase flow.
	 * @param purchaseTicket The purchase ticket in base64 encoded form. It is SHA1 (160 bit) hash.
	 *                       Hash is calculated over the string, which is derived by concatenating the attribute values.
	 *                       The attribute values are concatenated in order transactionId, transactionTime, productId, applicationId, accountId, IMEI and IMSI
	 */
	void purchaseCompleted(int status, String purchaseTicket);

	/**
	 * This callback function is executed when the IAPClientPaymentProvider gets information from the Nokia Store
	 * back end indicating that the item's restoration is possible.
	 * <p>
	 *
	 * @param status         The status of the restoration flow.
	 * @param purchaseTicket The purchase ticket in base64 encoded form. It is SHA1 (160 bit) hash.
	 *                       Hash is calculated over the string, which is derived by concatenating the attribute values.
	 *                       The attribute values are concatenated in order transactionId, transactionTime, productId, applicationId, accountId, IMEI and IMSI.
	 */
	void restorationCompleted(int status, String purchaseTicket);

	/**
	 * This callback function is executed when the request for products available for restoration is complete.
	 * <p>
	 * The results returned by the list of IAPClientProductData objects will be valid only during the scope of the callback method.
	 * The application will receive unpredictable results if it attempts to use the objects at any other time.
	 * <p>
	 *
	 * @param status          The status of the getRestorableProducts() request.
	 * @param productDataList The list of IAPClientProductData objects containing the metadata of the requested product.
	 */
	void restorableProductsReceived(int status, IAPClientProductData[] productDataList);

	/**
	 * This callback function is executed when the user and device data request is completed.
	 * <p>
	 * The results returned by the IAPClientUserAndDeviceData object will be valid only during the scope of the callback method.
	 * The application will receive unpredictable results if it attempts to use the object at any other time.
	 * <p>
	 *
	 * @param status The status of the getUserAndDeviceId() request.
	 * @param ud     The IAPClientUserAndDeviceData class containing User and Device Data received from Nokia Store backend.
	 */
	void userAndDeviceDataReceived(int status, IAPClientUserAndDeviceData ud);
}
