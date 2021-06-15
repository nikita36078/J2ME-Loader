package com.sonivox.mmapi;

import javax.microedition.media.*;
import java.io.*;

/**
 * Basic security framework for MMAPI
 */
class Security {

	/**
	 * Verify read access to the given locator. This is especially important
	 * for "capture" protocols.
	 * 
	 * @param locator the locator to test for access permission
	 * @throws SecurityException if the locator cannot be accessed due to
	 *             security restrictions
	 */
	static void checkLocatorAccess(String locator) throws SecurityException {

	}

	/**
	 * Verify write access to OutputStream for the given Player. 
	 * This is especially important for "capture" protocols.
	 * 
	 * @param player the player of which data is asked to be recorded to an OutputStream
	 * @param stream the OutputStream to which the media is asked to be written
	 * 
	 * @throws SecurityException if writing the player's content to an OutputStream is not permitted due to
	 *             security restrictions
	 */
	static void checkRecordPermission(Player player, OutputStream stream) throws SecurityException {

	}

	/**
	 * Verify write access to the given locator for the given Player. 
	 * This is especially important for "capture" protocols.
	 * 
	 * @param player the player of which data is asked to be recorded to the locator
	 * @param locator the locator to which the media is asked to be written
	 * 
	 * @throws SecurityException if writing the player's content to the locator is not permitted due to
	 *             security restrictions
	 */
	static void checkRecordPermission(Player player, String locator) throws SecurityException {

	}

}
