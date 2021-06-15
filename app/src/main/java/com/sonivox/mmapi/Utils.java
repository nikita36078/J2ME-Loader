package com.sonivox.mmapi;

import javax.microedition.media.*;

/**
 * Common utility methods for the MMAPI implementation.
 */
class Utils {

	/**
	 * Parses the specified locator (URL) and returns the protocol part. The
	 * returned protocol is converted to lower case.
	 * 
	 * @param locator the URL to parse
	 * @return the protocol, or null if the locator does not have a protocol
	 *         portion
	 */
	static String getProtocol(String locator) {
		if (locator == null) {
			return null;
		}
		int i = locator.indexOf(':');
		if (i > 0) {
			// need at least one character in the protocol
			return locator.substring(0, i).toLowerCase();
		}
		return null;
	}

	/**
	 * Parses the specified locator (URL) and returns the extension part. The
	 * returned extension is converted to lower case.
	 * 
	 * @param locator the URL to parse
	 * @return the lower case extension without dot, or null if the locator does
	 *         not have a extension portion
	 */
	static String getExtension(String locator) {
		if (locator == null) {
			return null;
		}
		int i = locator.lastIndexOf('.');
		if ((i > 0) && (locator.length() > (i + 1))) {
			// the part before the extension must be at least one character,
			// and the extension must be at least one character, too.
			return locator.substring(i + 1).toLowerCase();
		}
		return null;
	}

	/**
	 * Parse the specified capture: or device: locator for the device name. E.g.
	 * for the locator <code>capture://audio?encoding=pcm</code> it returns
	 * <code>audio</code>.
	 * 
	 * @param locator the locator to parse, must be device or capture protocol
	 * @return the device, or null if there is no device specified
	 */
	static String getDevice(String locator) {
		if (locator == null) {
			return null;
		}
		int i = locator.indexOf("://");
		if ((i > 0) && (locator.length() > (i + 3))) {
			// the protocol part before the device must be at least one
			// character,
			// and the device name must be at least one character, too.
			String device = locator.substring(i + 3);
			// the device is terminated by a ?
			i = device.indexOf("?");
			if (i > 0) {
				// require at least one character in device name
				return device.substring(0, i);
			} else if (i < 0) {
				return device;
			}
		}
		return null;
	}

	/**
	 * Parse the specified capture: or device: locator for the specified
	 * parameter. E.g.
	 * <code>getLocatorParameter(&quot;capture://audio?encoding=pcm&quot;, &quot;encoding&quot;)</code>
	 * returns <code>pcm</code>.
	 * 
	 * @param locator the locator to parse, must be device or capture protocol
	 * @param param the parameter to find in the locator
	 * @param def A default value, or null if none is wanted
	 * @return the value of the parameter, or the empty string if the parameter
	 *         is specified in the locator but does not have a value, or the
	 *         specified default value if the parameter does not exist in the
	 *         locator
	 */
	static String getParameterValue(String locator, String param, String def) {
		if (locator == null) {
			return def;
		}
		// all parameters must follow the ? character
		int after = locator.indexOf("?");

		while (true) {
			int i = locator.indexOf(param);
			if (i <= after) {
				return def;
			}
			int nextCharIndex = i + param.length();

			// if there is no following character, or the following character is
			// the & character, we have a parameter without value
			if (nextCharIndex >= locator.length()
					|| locator.charAt(nextCharIndex) == '&') {
				return "";
			}

			// if the following char is not the equals sign, the parameter name
			// is different and we need to restart the search
			if (locator.charAt(nextCharIndex) != '=') {
				locator = locator.substring(nextCharIndex);
				after = -1;
				continue;
			}

			// otherwise the value is after the equals sign
			String value = locator.substring(nextCharIndex + 1);

			// the param is separated by a &
			i = value.indexOf('&');
			if (i >= 0) {
				return value.substring(0, i);
			}
			return value;
		} // while loop
	}

	/**
	 * Parse the specified capture: or device: locator for the specified integer
	 * parameter. E.g.
	 * <code>getLocatorParameter(&quot;capture://audio?rate=44100&quot;, 8000)</code>
	 * returns <code>pcm</code>.
	 * 
	 * @param locator the locator to parse, must be device or capture protocol
	 * @param param the parameter to find in the locator
	 * @param def A default value
	 * @return the integer value of the parameter, or the default value if the
	 *         specified default value if the parameter does not exist in the
	 *         locator
	 * @throws MediaExsception if the parameter is specified in the locator but
	 *             does not have a value, or if the value is not a number
	 */
	static int getParameterValue(String locator, String param, int def)
			throws MediaException {
		String sValue = getParameterValue(locator, param, null);
		if (sValue == null) {
			return def;
		}
		if (sValue != "") {
			try {
				return Integer.parseInt(sValue);
			} catch (NumberFormatException nfe) {
				// nothing to do
			}
		}
		throw new MediaException("illegal value for " + param);
	}
}
