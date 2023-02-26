package com.nokia.mid.payment;

import java.io.IOException;
import java.io.InputStream;

public class DrmResourceInputStream extends InputStream {

	/**
	 * Reads the next byte of data from the input stream.
	 *
	 * @return the next byte of data, or <code>-1</code> if the end of the stream is reached.
	 * @throws IOException - if an I/O error occurs.
	 */
	public int read() throws IOException {
		return 0;
	}

	/**
	 * Closes this input stream and releases any system resources associated with the stream.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	public void close() throws IOException {
	}

	/**
	 * Returns the number of bytes that can be read (or skipped over) from
	 * this input stream without blocking by the next caller of a method for
	 * this input stream.  The next caller might be the same thread or
	 * another thread.
	 *
	 * @return the number of bytes that can be read from this input stream without blocking.
	 * @throws IOException if an I/O error occurs.
	 */
	public int available() throws IOException {
		return 0;
	}

	/**
	 * Reads up to <code>len</code> bytes of data from the input stream into
	 * an array of bytes.  An attempt is made to read as many as
	 * <code>len</code> bytes, but a smaller number may be read, possibly
	 * zero. The number of bytes actually read is returned as an integer.
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
	 * <p> In every case, elements <code>b[0]</code> through
	 * <code>b[off]</code> and elements <code>b[off+len]</code> through
	 * <code>b[b.length-1]</code> are unaffected.
	 *
	 * <p> If the first byte cannot be read for any reason other than end of
	 * file, then an <code>IOException</code> is thrown. In particular, an
	 * <code>IOException</code> is thrown if the input stream has been closed.
	 *
	 * @param b   the buffer into which the data is read.
	 * @param off the start offset in array <code>b</code> at which the data is written.
	 * @param len the maximum number of bytes to read.
	 * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no more data because the end of the stream has been reached.
	 * @throws IOException if an I/O error occurs.<DT>
	 * @see InputStream#read()
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		return 0;
	}

	/**
	 * Marks the current position in this input stream. A subsequent call to
	 * the <code>reset</code> method repositions this stream at the last marked
	 * position so that subsequent reads re-read the same bytes.
	 *
	 * <p> The <code>readlimit</code> arguments tells this input stream to
	 * allow that many bytes to be read before the mark position gets
	 * invalidated. This is not used in this implementation.
	 *
	 * <p> The general contract of <code>mark</code> is that, if the method
	 * <code>markSupported</code> returns <code>true</code>, the stream somehow
	 * remembers all the bytes read after the call to <code>mark</code> and
	 * stands ready to supply those same bytes again if and whenever the method
	 * <code>reset</code> is called.  However, the stream is not required to
	 * remember any data at all if more than <code>readlimit</code> bytes are
	 * read from the stream before <code>reset</code> is called.
	 * <p>
	 *
	 * @param readlimit the maximum limit of bytes that can be read before the mark position becomes invalid.
	 * @see InputStream#reset()
	 */
	public void mark(int readlimit) {

	}

	/**
	 * Tests if this input stream supports the <code>mark</code> and
	 * <code>reset</code> methods. The <code>markSupported</code> method of
	 * <code>InputStream</code> returns <code>true</code>.
	 * <p>
	 *
	 * @return <code>true</code> if this true type supports the mark and reset method; <code>false</code> otherwise.
	 * @see InputStream#mark(int)
	 * @see InputStream#reset()
	 */
	public boolean markSupported() {
		return false;
	}

	/**
	 * Repositions this stream to the position at the time the
	 * <code>mark</code> method was last called on this input stream.
	 *
	 * <p> The general contract of <code>reset</code> is:
	 * <p>
	 * <ul><li> If the method <code>markSupported</code> returns <code>true</code>, then:
	 * <ul><li> If the method <code>mark</code> has not been called since
	 * the stream was created, or the number of bytes read from the stream
	 * since <code>mark</code> was last called is larger than the argument
	 * to <code>mark</code> at that last call, then an
	 * <code>IOException</code> might be thrown.
	 *
	 * <li> If such an <code>IOException</code> is not thrown, then the
	 * stream is reset to a state such that all the bytes read since the
	 * most recent call to <code>mark</code> (or since the start of the
	 * file, if <code>mark</code> has not been called) will be resupplied
	 * to subsequent callers of the <code>read</code> method, followed by
	 * any bytes that otherwise would have been the next input data as of
	 * the time of the call to <code>reset</code>. </ul>
	 *
	 * <li> If the method <code>markSupported</code> returns
	 * <code>false</code>, then:
	 *
	 * <ul><li> The call to <code>reset</code> may throw an
	 * <code>IOException</code>.
	 *
	 * <li> If an <code>IOException</code> is not thrown, then the stream
	 * is reset to a fixed state that depends on the particular type of the
	 * input stream and how it was created. The bytes that will be supplied
	 * to subsequent callers of the <code>read</code> method depend on the
	 * particular type of the input stream. </ul></ul>
	 * <p>
	 *
	 * @throws IOException if this stream has not been marked or if the mark has been invalidated.
	 * @see InputStream#mark(int)
	 * @see IOException
	 */
	public void reset() throws IOException {
	}

	/**
	 * Skips over and discards <code>n</code> bytes of data from this input
	 * stream. The <code>skip</code> method may, for a variety of reasons, end
	 * up skipping over some smaller number of bytes, possibly <code>0</code>.
	 * This may result from any of a number of conditions; reaching end of file
	 * before <code>n</code> bytes have been skipped is only one possibility.
	 * The actual number of bytes skipped is returned.  If <code>n</code> is
	 * negative, no bytes are skipped.
	 * <p>
	 *
	 * @param n the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @throws IOException if an I/O error occurs.
	 */
	public long skip(long n) throws IOException {
		return 0;
	}
}
