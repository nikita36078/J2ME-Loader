package com.nokia.mid.payment;

import java.io.InputStream;

/**
 * <p>
 * This is the main class for initiating and managing the purchase process for the purchaseable items in an application. Its methods
 * are asynchronous with responses received from the underlying implementation callbacks
 * All API methods except getProductData() will initiate the user authentication
 * by Single-Sign On (SSO).The IAPClientPaymentManager class handles a single API function call at a time. If a second request is
 * made while response is pending from a previous function call, a failure value is returned indicating that this request shall not
 * be processed and no callback can be expected for this request.
 *
 * <pre>
 * sample code snippet
 *  package sample;
 *
 *
 *  import javax.microedition.midlet.*;
 *  import javax.microedition.lcdui.*;
 *  import com.nokia.mid.payment.*;
 *
 *  import java.io.*;
 *
 *  public class SampleMIDlet extends MIDlet implements CommandListener, IAPClientPaymentListener
 *  {
 *
 *      private Display display;     // The display for this MIDlet
 *      private IAPClientPaymentManager manager;  //Singleton instance of IAPClientPaymentManager() class
 *
 *      public SampleMIDlet()
 *
 *      {
 *          display = Display.getDisplay(this);
 *      }
 *
 *
 *      public void startApp()
 *      {
 *          int status = 0;
 *
 *
 *          try {
 *              // set IAPClientPaymentListener
 *              manager = IAPClientPaymentManager.getIAPClientPaymentManager();
 *              manager.setIAPClientPaymentListener(this);
 *
 *              // request metadata for product "game_level_3"
 *              status = manager.getProductData("123456");
 *
 *              if (status != IAPClientPaymentManager.SUCCESS) {
 *
 *                  // Do not expect a productDataReceived() callback with the requested metadata, handle the fail case
 *              }
 *          } catch (IAPClientPaymentException e) {
 *              // handle IAPClientPaymentException from getIAPClientPaymentManager()
 *
 *          }
 *
 *          status = manager.purchaseProduct("123456", IAPClientPaymentManager.FORCED_AUTOMATIC_RESTORATION);
 *
 *          if(status != IAPClientPaymentManager.SUCCESS)
 *          {
 *              // Do not expect a purchaseCompleted() asynchronous callback, handle the fail case
 *
 *          }
 *
 *          // restore product "game_level_5"
 *
 *          status = manager.restoreProduct("112233", IAPClientPaymentManager.ONLY_IN_SILENT_AUTHENTICATION);
 *          if(status != IAPClientPaymentManager.SUCCESS)
 *          {
 *
 *              // Do not expect a restoreCompleted() asynchronous callback, handle the fail case
 *          }
 *      }
 *
 *
 *      public void pauseApp()
 *      {
 *      }
 *
 *      public void destroyApp(boolean unconditional)
 *      {
 *
 *      }
 *
 *
 *      public void commandAction(Command c, Displayable s)
 *      {
 *      }
 *
 *      public void productDataReceived(int status, IAPClientProductData pd)
 *      {
 *          if(status == OK)
 *          {
 *
 *               String title = pd.getTitle();
 *               String price = pd.getPrice();
 *          }
 *
 *          //Update UI with information
 *      }
 *
 *      public void purchaseCompleted(int status, String purchaseTicket)
 *      {
 *
 *          if(status == OK)
 *          {
 *               InputStream input = manager.getDRMResourceAsStream("/res/drm/data/ResourceId_123456");
 *               // Unlock game level 3, allow to be used
 *          }
 *      }
 *
 *      public  void restorableProductsReceived(int status, IAPClientProductData[] productDataList)
 *
 *      {
 *          if(status == OK)
 *          {
 *               for (int i = 0; i < productDataList.length; i++) {
 *                   // Access each product data:productDataList[i]
 *               }
 *          }
 *      }
 * </pre>
 * <p>
 */
public final class IAPClientPaymentManager extends Object {

	/**
	 * The operation is executed as a normal one. The user authentication is requested when necessary.
	 */
	public static final int DEFAULT_AUTHENTICATION = 0;

	/**
	 * The operation is executed silently without the user authentication. If this is not possible, the operation is aborted
	 * If the user must enter account credentials, for example an account password, while ONLY_IN_SILENT_AUTHENTICATION
	 * is passed, the operation is aborted. Otherwise, the operation continues regardless of whether
	 * the user has to enter account credentials or not.
	 */
	public static final int ONLY_IN_SILENT_AUTHENTICATION = 1;

	/**
	 * The purchase request does not restore the item.
	 */
	public static final int NO_FORCED_RESTORATION = 0;

	/**
	 * The purchase request is automatically transformed to a restoration if item restoration is available.
	 */
	public static final int FORCED_AUTOMATIC_RESTORATION = 1;

	/**
	 * The operation is successful, expect an asynchronous callback at a later time when requested information is available.
	 */
	public static final int SUCCESS = 1;

	/**
	 * The operation has failed, do not  expect an asynchronous callback later.
	 */
	public static final int GENERAL_FAIL = -1;

	public static final int PENDING_REQUEST = -2;
	public static final int NULL_INPUT_PARAMETER = -3;
	public static final int KNI_INTERNAL_FAIL = -4;
	public static final int OUT_OF_MEMORY = -5;
	public static final int TEST_SERVER = 1;
	public static final int SIMULATION = 2;
	public static final int PURCHASE = 101;
	public static final int RESTORE = 102;
	public static final int FAIL = 103;
	public static final int NORMAL = 104;

	/**
	 * Retrieve the singleton IAPClientPaymentManager instance
	 *
	 * @return The return is the instance of IAPClientPaymentManager singleton: The value is null if the singleton has already been initialized
	 * @throws IAPClientPaymentException if the Application ID can't be read out from JAR package
	 * @throws SecurityException         if the caller does not have security permission to use In-App Purchase API
	 */
	public static IAPClientPaymentManager getIAPClientPaymentManager() throws IAPClientPaymentException {
		return InstanceHolder.instance;
	}

	/**
	 * Set up a listener for IAP related events from the observed class IAPClientPaymentManager.
	 *
	 * @param iapListener of type IAPClientPaymentListener
	 */
	public static void setIAPClientPaymentListener(IAPClientPaymentListener iapListener) {

	}

	/**
	 * Provides information about purchasable items for the publisher application for building a catalog list. The productDataReceived() callback returns a ProductData structure
	 * for each of the productIDs.
	 * The callback is sent only if the return value is SUCCESS.
	 * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
	 * <p>
	 *
	 * @param productId The productId of the purchaseable items. Publishers register sub-products(IAP) with Nokia store to receive product IDs
	 * @return The return is an integer value. The value is one of the following:
	 * <li> SUCCESS: The method call succeeded. IAP API sends the productDataReceived() callback to the publisher application. .
	 * <li> FAIL: The method call failed. IAP API does not send the productDataReceived() callback to the publisher application.
	 */
	public int getProductData(String productId) {
		return FAIL;
	}

	/**
	 * Provides information about purchasable items for the publisher application for building a catalog list. The productDataListReceived() callback returns ProductData structure
	 * for a list of requested productIDs.
	 * The callback is sent only if the return value is SUCCESS.
	 * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in the future.
	 * <p>
	 *
	 * @param productIdList The productIds of a list of purchaseable items. Publishers register sub-products(IAP) with Nokia store to receive product IDs
	 * @return The return is an integer value. The value is one of the following:
	 * <li> SUCCESS: The method call succeeded. IAP API sends the productDataListReceived() callback to the publisher application. .
	 * <li> FAIL: The method call failed. IAP API does not send the productDataListReceived() callback to the publisher application.
	 */
	public int getProductData(String[] productIdList) {
		return FAIL;
	}

	/**
	 * <p>Initiates the purchase flow. If forceRestorationFlag is set and the item to be purchased is restorable, the purchase flow is automatically
	 * transformed to a restoration flow. Only Nokia DRM encrypted items can be restored using IAPClient API.
	 *
	 * <p>The function call results in the  purchaseCompleted() callback during execution if the return value is positive:
	 * This provides the purchase result, when the Nokia Store back end has completed the purchase
	 * and the purchase data is available for the client application.
	 * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
	 * <p>
	 *
	 * @param productId            The ID of the product to be purchased. Publishers register sub-products(IAP) with Nokia store to receive product IDs
	 * @param forceRestorationFlag the flag for selecting forcedRestoration when available
	 *                             Possible values are one of the constants defined in this class, {@link #NO_FORCED_RESTORATION} or {@link #FORCED_AUTOMATIC_RESTORATION}
	 * @return The return is an integer value. The value is one of the following:
	 * <li> {@link #SUCCESS}: The method call succeeded. IAP API sends the purchaseCompleted() callback to the publisher application.
	 * <li> {@link #FAIL}: The method call failed. IAP API does not send the purchaseCompleted() callback to the publisher application.
	 */
	public int purchaseProduct(String productId, int forceRestorationFlag) {
		return FAIL;
	}

	/**
	 * Initiates the restoration flow. Only Nokia DRM encrypted items can be restored using IAPClient API.
	 * <p>
	 * The function call results in the restorationCompleted() callback during the execution if the return value is positive
	 * This provides the result of the restoration, when it is completed by the Nokia Store back end
	 * and the restoration data is available for the client application.
	 * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
	 * <p>
	 *
	 * @param productId          The ID of the product to be restored.  Publishers register sub-products(IAP) with Nokia store to receive product IDs
	 * @param authenticationMode Defines whether product is being restored silently without asking the user authentication or not
	 *                           Possible values are one of the constants defined in this class, DEFAULT_AUTHENTICATION or ONLY_IN_SILENT_AUTHENTICATION
	 * @return The return is an integer value. The value is one of the following:
	 * <li> SUCCESS: The method call succeeded. IAP API sends the restorationCompleted() callback to the publisher application.
	 * <li> FAIL: The method call failed. IAP API does not send the restorationCompleted() callback to the publisher application.
	 */
	public int restoreProduct(String productId, int authenticationMode) {
		return FAIL;
	}

	/**
	 * Provides a list of Nokia Store items related to the given application ID that are restorable via IAPClient API. The restorableProductsReceived() callback
	 * function returns the list of restorable products.
	 * The callback function is called only if the return value is positive.
	 * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
	 * <p>
	 *
	 * @param authenticationMode Defines whether restorable products are fetched silently without asking the user authentication or not
	 * @return The return is an integer value. The value is one of the following:
	 * <li> SUCCESS: The method call succeeded. IAP API sends the restorableProductsReceived() callback to the publisher application. .
	 * <li> FAIL: The method call failed. IAP API does not send the restorableProductsReceived() callback to the publisher application.
	 */
	public int getRestorableProducts(int authenticationMode) {
		return FAIL;
	}

	/**
	 * Provides all necessary information about the current user and the device for the publisher service to restore non-Nokia DRM encrypted data
	 * to the device. The userAndDeviceDataReceived() callback returns the data in hashed form. The callback is sent only if the return value is positive.
	 * Upon success the method schedules a callback which is dispatched to the application at some suitable time instance in future.
	 * <p>
	 *
	 * @param authenticationMode Defines whether the user and the device data are fetched silently without asking user authentication or not
	 *                           Possible values are one of the constants defined in this class, DEFAULT_AUTHENTICATION or ONLY_IN_SILENT_AUTHENTICATION
	 * @return The return is an integer value. The value is one of the following:
	 * <li> SUCCESS: The method call succeeded. IAP API sends the userAndDeviceDataReceived() callback to the publisher application. .
	 * <li> FAIL: The method call failed. IAP API does not send the userAndDeviceDataReceived() callback to the publisher application.
	 */
	public int getUserAndDeviceId(int authenticationMode) {
		return FAIL;
	}

	/**
	 * This provides the client application access to DRM encrypted resource files inside JAR.
	 * e.g. Possible location Jar-root/res/drm/data/level_1
	 * /level_2
	 * This is used after receiving the PurchaseTicket from a successful
	 * purchase flow or restoration flow.
	 * <p>
	 * Finds a drm resource with a given name inside the application's
	 * JAR file. This method returns
	 * <code>null</code> if no resource with this name is found
	 * in the application's JAR file.
	 * <p>
	 * The resource names can be represented in two
	 * different formats: absolute or relative.
	 * <p>
	 * Absolute format:
	 * <ul><code>/packagePathName/resourceName</code></ul>
	 * <p>
	 * Relative format:
	 * <ul><code>resourceName</code></ul>
	 * <p>
	 * In the absolute format, the programmer provides a fully
	 * qualified name that includes both the full path and the
	 * name of the resource inside the JAR file.  In the path names,
	 * the character "/" is used as the separator.
	 * <p>
	 * In the relative format, the programmer provides only
	 * the name of the actual resource.  Relative names are
	 * converted to absolute names by the system by prepending
	 * the resource name with the fully qualified package name
	 * of class upon which the <code>geDRMResourceAsStream</code>
	 * method was called.
	 * <p>
	 *
	 * @param name name of the desired resource
	 * @return a <code>java.io.InputStream</code> object.
	 */
	public InputStream getDRMResourceAsStream(String name) {
		return null;
	}

	private static final class InstanceHolder {
		static final IAPClientPaymentManager instance = new IAPClientPaymentManager();
	}
}
