package com.sonivox.mmapi;

import javax.microedition.media.*;

/**
 * The default system time base, as returned by Manager.getSystemTimeBase().
 */
class SystemTimeBase implements TimeBase {

	private final static long offset = System.currentTimeMillis() * 1000L;

	/**
	 * This implementation returns converted System.currentTimeMillis(), minus
	 * an offset so that the system time base starts at 0.
	 */
	public long getTime() {
		return System.currentTimeMillis() * 1000L - offset;
	}

}
