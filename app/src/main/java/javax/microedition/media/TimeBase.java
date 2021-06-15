/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media;

/**
 * A <code>TimeBase</code> is a constantly ticking source of time.
 * It measures the progress of time and
 * provides the basic means for synchronizing media playback for
 * <code>Player</code>s.
 * <p>
 * A <code>TimeBase</code> measures time in microseconds in
 * order to provide the necessary resolution for synchronization.
 * It is acknowledged that some implementations may not be able to
 * support time resolution in the microseconds range.  For such
 * implementations, the internal representation of time can be done
 * within their limits.
 * But the time reported via the API must be scaled to the microseconds
 * range.
 * <p>
 * <code>Manager.getSystemTimeBase</code> provides the default
 * <code>TimeBase</code> used by the system.
 *
 * @see Player
 */
public interface TimeBase {

    /**
     * Get the current time of this <code>TimeBase</code>.  The values
     * returned must be non-negative and non-decreasing over time.
     *
     * @return the current <code>TimeBase</code> time in microseconds.
     */
    long getTime();
}
