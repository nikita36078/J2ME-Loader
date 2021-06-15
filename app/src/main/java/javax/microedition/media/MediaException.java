/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media;

/**
 * A <code>MediaException</code> indicates an unexpected error
 * condition in a method.
 *
 */

public class MediaException extends Exception {

    /**
     * Constructs a <code>MediaException</code> with <code>null</code>
     * as its error detail message.
     */
    public MediaException() {
	super();
    }

    /**
     * Constructs a <code>MediaException</code> with the specified detail
     * message. The error message string <code>s</code> can later be
     * retrieved by the
     * <code>{@link Throwable#getMessage}</code>
     * method of class <code>java.lang.Throwable</code>.
     *
     * @param reason the detail message.
     */
    public MediaException(String reason) {
	super(reason);
    }
}
