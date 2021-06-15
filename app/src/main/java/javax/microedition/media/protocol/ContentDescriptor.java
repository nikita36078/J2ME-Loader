/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.protocol;

/**
 * A <CODE>ContentDescriptor</CODE> identifies media data containers.
 *
 * @see SourceStream
 */

public class ContentDescriptor {

    private String encoding;

    /**
     * Obtain a string that represents the content type
     * for this descriptor.
     * If the content type is not known, <code>null</code> is returned.
     *
     * @return The content type.
     */
    public String getContentType() {
	return encoding;
    }

    /**
     * Create a content descriptor with the specified content type.
     *
     * @param contentType The content type of this descriptor.
     * If <code>contentType</code> is <code>null</code>, the type
     * of the content is unknown.
     */
    public ContentDescriptor(String contentType) {
	encoding = contentType;
    }
}
