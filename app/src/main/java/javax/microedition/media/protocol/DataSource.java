/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.protocol;

import javax.microedition.media.Controllable;
//import javax.microedition.media.Control;
import java.io.IOException;

/**
 * A <CODE>DataSource</CODE> is an abstraction for media protocol-handlers.
 * It hides the details of how the data is read from source--whether
 * the data is
 * coming from a file, streaming server or proprietary delivery mechanism.
 * It provides the methods for a <code>Player</code> to access
 * the input data.
 * <p>
 * An application-defined protocol can be implemented with a custom
 * <code>DataSource</code>.  A <code>Player</code> can then be
 * created for playing back the media from the custom
 * <code>DataSource</code> using the
 * <a href="../Manager.html#createPlayer(javax.microedition.media.protocol.DataSource)">
 * <code>Manager.createPlayer</code></a> method.
 * <p>
 * There are a few reasons why one would choose to implement
 * a <code>DataSource</code> as opposed to an <code>InputStream</code>
 * for a custom protocol:
 * <ul>
 * <li>
 * <code>DataSource/SourceStream</code> provides the random
 * seeking API that
 * is not supported by an <code>InputStream</code>.  i.e., if
 * the custom protocol
 * requires random seeking capabilities, a custom
 * <code>DataSource</code> can be used.
 * <li>
 * <code>DataSource/SourceStream</code> supports the concept of
 * transfer size
 * that is more suited for frame-delimited data, e.g. video.
 * </ul>
 * <p>
 * A <code>DataSource</code> contains a set of <code>SourceStream</code>s.
 * Each <code>SourceStream</code> represents one elementary data stream
 * of the source.  In the most common case, a <code>DataSource</code>
 * only provides one <code>SourceStream</code>.  A <code>DataSource</code>
 * may provide multiple <code>SourceStream</code>s if it encapsulates
 * multiple elementary data streams.
 * <p>
 * Each of the <code>SourceStream</code>s provides the methods to allow
 * a <code>Player</code> to read data for processing.
 * <p>
 * <CODE>DataSource</CODE> manages the life-cycle of the media source
 * by providing a simple connection protocol.
 *
 * <p>
 * <a name="controls">
 * <code>DataSource</code> implements <code>Controllable</code> which
 * provides extra controls via some type-specific <code>Control</code>
 * interfaces.  <code>getControl</code> and <code>getControls</code>
 * can only be called when the <code>DataSource</code> is connected.
 * An <code>IllegalStateException</code> will be thrown otherwise.
 *
 * @see javax.microedition.media.Manager
 * @see SourceStream
 * @see ContentDescriptor
 */
abstract public class DataSource implements Controllable {

    private String sourceLocator;

    /**
     * Construct a <CODE>DataSource</CODE> from a locator.
     * This method should be overloaded by subclasses;
     * the default implementation just keeps track of
     * the locator.
     *
     * @param locator The locator that describes
     * the <CODE>DataSource</CODE>.
     */
    public DataSource(String locator) {
	sourceLocator = locator;
    }

    /**
     * Get the locator that describes this source.
     * Returns <CODE>null</CODE> if the locator hasn't been set.
     *
     * @return The locator for this source.
     */
    public String getLocator() {
	return sourceLocator;
    }

    /**
     * Get a string that describes the content-type of the media
     * that the source is providing.
     *
     * @return The name that describes the media content.
     * Returns <code>null</code> if the content is unknown.
     * @exception IllegalStateException Thrown if the source is
     * not connected.
     */
    public abstract String getContentType();

    /**
     * Open a connection to the source described by
     * the locator and initiate communication.
     *
     * @exception IOException Thrown if there are IO problems
     * when <CODE>connect</CODE> is called.
     * @exception SecurityException Thrown if the caller does not
     * have security permission to call <code>connect</code>.
     */
    public abstract void connect() throws IOException;

    /**
     * Close the connection to the source described by the locator
     * and free resources used to maintain the connection.
     * <p>
     * If no resources are in use, <CODE>disconnect</CODE> is ignored.
     * If <CODE>stop</CODE> hasn't already been called,
     * calling <CODE>disconnect</CODE> implies a stop.
     *
     */
    public abstract void disconnect();

    /**
     * Initiate data-transfer. The <CODE>start</CODE> method must be
     * called before data is available for reading.
     *
     * @exception IllegalStateException Thrown if the
     * <code>DataSource</code> is not connected.
     * @exception IOException Thrown if the <code>DataSource</code>
     * cannot be started due to some IO problems.
     * @exception SecurityException Thrown if the caller does not
     * have security permission to call <code>start</code>.
     */
    public abstract void start() throws IOException;

    /**
     * Stop the data-transfer.
     * If the <code>DataSource</code> has not been connected and started,
     * <CODE>stop</CODE> is ignored.
     *
     * @exception IOException Thrown if the <code>DataSource</code>
     * cannot be stopped due to some IO problems.
     */
    public abstract void stop() throws IOException;

    /**
     * Get the collection of streams that this source
     * manages. The collection of streams is entirely
     * content dependent. The  MIME type of this
     * <CODE>DataSource</CODE> provides the only indication of
     * what streams may be available on this connection.
     *
     * @return The collection of streams for this source.
     * @exception IllegalStateException Thrown if the source
     * is not connected.
     */
    public abstract SourceStream[] getStreams();

    //public abstract Control [] getControls();

    //public abstract Control getControl(String controlType);
}
