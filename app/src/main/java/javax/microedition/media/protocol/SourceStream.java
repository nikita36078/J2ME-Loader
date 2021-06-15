/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.protocol;

import java.io.IOException;
import javax.microedition.media.Controllable;

/**
 * Abstracts a single stream of media data.  It is used in
 * conjunction with <code>DataSource</code> to provide the
 * input interface to a <code>Player</code>
 * <p>
 * SourceStream may provide type-specific controls.  For that
 * reason, it implements the <code>Controllable</code> interface
 * to provide additional controls.
 *
 * @see DataSource
 *
 */

public interface SourceStream extends Controllable {

    /**
     * The value returned by <code>getSeekType</code> indicating that this
     * <code>SourceStream</code> is not seekable.
     * <p>
     * Value 0 is assigned to <code>NOT_SEEKABLE</code>.
     */
    int NOT_SEEKABLE = 0;

    /**
     * The value returned by <code>getSeekType</code> indicating that this
     * <code>SourceStream</code> can be seeked only to the beginning
     * of the media stream.
     * <p>
     * Value 1 is assigned to <code>SEEKABLE_TO_START</code>.
     */
    int SEEKABLE_TO_START = 1;

    /**
     * The value returned by <code>getSeekType</code> indicating that this
     * <code>SourceStream</code> can be seeked anywhere within the media.
     * <p>
     * Value 2 is assigned to <code>RANDOM_ACCESSIBLE</code>.
     */
    int RANDOM_ACCESSIBLE = 2;

    /**
     * Get the content type for this stream.
     *
     * @return The current <CODE>ContentDescriptor</CODE> for this stream.
     */
    ContentDescriptor getContentDescriptor();


    /**
     * Get the size in bytes of the content on this stream.
     *
     * @return The content length in bytes.  -1 is returned if the
     * length is not known.
     */
    long getContentLength();


    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     *
     * <p> If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     *
     * <p> If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
     * thrown.
     *
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     *
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p> If the first byte cannot be read for any reason other than end of
     * file, then an <code>IOException</code> is thrown. In particular, an
     * <code>IOException</code> is thrown if the input stream has been closed.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     * @exception  IOException  if an I/O error occurs.
     */
    int read(byte[] b, int off, int len)
	throws IOException;


    /**
     * Get the size of a "logical" chunk of media data from the source.
     * This method can be used to determine the minimum size of the
     * buffer to use in conjunction with the <code>read</code> method
     * to read data from the source.
     *
     * @return The minimum size of the buffer needed to read a "logical"
     * chunk of data from the source.  Returns -1 if the size cannot be
     * determined.
     * @see #read(byte[], int, int)
     */
    int getTransferSize();


    /**
     * Seek to the specified point in the stream.  The <code>seek</code>
     * method may, for a variety of reasons, fail to seek to the specified
     * position.  For example,
     * it may be asked to seek to a position beyond the size of the stream;
     * or the stream may only be seekable to the beginning
     * (<code>getSeekType</code> returns <code>SEEKABLE_TO_START</code>).
     * The return value indicates whether the seeking is successful.
     * If it is successful, the value returned will be the same as the
     * given position.  Otherwise, the return value will indicate what
     * the new position is.
     * <p>
     * If the given position is negative, seek will treat that as 0
     * and attempt to seek to 0.
     * <p>
     * An IOException will be thrown if an I/O error occurs, e.g. when
     * the stream comes from a remote connection and the connection is
     * broken.
     *
     * @param where The position to seek to.
     * @return The new stream position.
     * @exception IOException Thrown if an I/O error occurs.
     */
    long seek(long where) throws IOException;


    /**
     * Obtain the current position in the stream.
     * @return The current position in the stream.
     */
    long tell();


    /**
     * Find out if the stream is seekable.
     * The return value can be one of these three:
     * <code>NOT_SEEKABLE</code>, <code>SEEKABLE_TO_START</code> and
     * <code>RANDOM_ACCESSIBLE</code>.
     * If the return value is <code>SEEKABLE_TO_START</code>, it means
     * that the stream can only be repositioned to the beginning of
     * the stream.  If the return value is <code>RANDOM_ACCESSIBLE</code>,
     * the stream can be seeked anywhere within the stream.
     *
     * @return Returns an enumerated value to indicate the level of seekability.
     */
    int getSeekType();
}
