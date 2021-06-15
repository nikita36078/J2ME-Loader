/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;

import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.media.MediaException;


/**
 * <code>RecordControl</code> controls the recording of media
 * from a <code>Player</code>.  <code>RecordControl</code> records
 * what's currently being played by the <code>Player</code>.
 * <p>
 * <h2>Example</h2>
 * <blockquote>
 * <pre>
 * try {
 *    // Create a Player that captures live audio.
 *    Player p = Manager.createPlayer("capture://audio");
 *    p.realize();
 *    // Get the RecordControl, set the record stream,
 *    // start the Player and record for 5 seconds.
 *    RecordControl rc = (RecordControl)p.getControl("RecordControl");
 *    ByteArrayOutputStream output = new ByteArrayOutputStream();
 *    rc.setRecordStream(output);
 *    rc.startRecord();
 *    p.start();
 *    Thread.currentThread().sleep(5000);
 *    rc.commit();
 *    p.close();
 * } catch (IOException ioe) {
 * } catch (MediaException me) {
 * } catch (InterruptedException ie) { }
 * </pre>
 * </blockquote>
 *
 * @see javax.microedition.media.Player
 */
public interface RecordControl extends javax.microedition.media.Control {

    /**
     * Set the output stream where the data will be
     * recorded.
     * <p>
     * Whenever possible, the recording format is the same as the format
     * of the input media.  In some cases, the recording format may be
     * different from the input format if the input format is not a
     * recordable format, e.g. streaming media data.  An application
     * can query the recorded format by calling the
     * <code>getContentType</code> method.
     *
     * @param stream The output stream where the data will be recorded.
     * @exception IllegalStateException Thrown if one of the following
     * conditions is true:
     * <ul>
     * <li>
     * <code>startRecord</code> has been called and <code>commit</code> has
     * not been called.
     * <li>
     * <code>setRecordLocation</code> has been called and <code>commit</code> has
     * not been called.
     * </ul>
     *
     * @exception IllegalArgumentException Thrown if
     * <code>stream</code> is null.
     * @exception SecurityException Thrown if the caller does not
     * have security permission to set the record stream.
     */
    void setRecordStream(OutputStream stream);

    /**
     * Set the output location where the data will be recorded.
     * <p>
     * Whenever possible, the recording format is the same as the format
     * of the input media.  In some cases, the recording format may be
     * different from the input format if the input format is not a
     * recordable format, e.g. streaming media data.  An application
     * can query the recorded format by calling the
     * <code>getContentType</code> method.
     *
     * @param locator The locator specifying where the
     * recorded media will be saved.  The locator must be
     * specified as a URL.
     * @exception IllegalStateException Thrown if one of the following
     * conditions is true:
     * <ul>
     * <li>
     * <code>startRecord</code> has been called and <code>commit</code> has
     * not been called.
     * <li>
     * <code>setRecordStream</code> has been called and <code>commit</code> has
     * not been called.
     * </ul>
     * @exception IllegalArgumentException Thrown if <code>locator</code>
     * is null.
     * @exception IOException Thrown if protocol is valid but the
     * media cannot be created at the specified location.
     * @exception MediaException Thrown if the locator is not in URL syntax
     * or it specifies a protocol that is not supported.
     * @exception SecurityException Thrown if the caller does not
     * have security permission to set the record location.
     */
    void setRecordLocation(String locator)
	throws IOException, MediaException;

    /**
     * Return the content type of the recorded media.
     *
     * The content type is given in the
     * <a HREF="../Manager.html#content-type">content type syntax</a>.
     *
     * @return The content type of the media.
     */
    String getContentType();

    /**
     * Start recording the media.
     * <p>
     * If the <code>Player</code> is already started, <code>startRecord</code>
     * will immediately start the recording.  If the <code>Player</code>
     * is not already started, <code>startRecord</code> will not
     * record any media.  It will put the recording in a "standby" mode.
     * As soon as the <code>Player</code> is started,
     * the recording will start right away.
     * <p>
     * If <code>startRecord</code> is called when the recording has
     * already started, it will be ignored.
     * <p>
     * When <code>startRecord</code> returns, the recording has started
     * and a <i>RECORD_STARTED</i> event will be delivered through the
     * <code>PlayerListener</code>.
     * <p>
     * If an error occurs while recording is in progress,
     * <i>RECORD_ERROR</i> event will be delivered via the PlayerListener.
     *
     * @exception IllegalStateException Thrown if any of the following
     * conditions is true:
     * <ul>
     * <li>
     * if <code>setRecordLocation</code> or <code>setRecordStream</code> has
     * not been called for the first time.
     * <li>
     * If <code>commit</code> has been called and
     * <code>setRecordLocation</code> or <code>setRecordStream</code>
     * has not been called.
     * </ul>
     */
    void startRecord();

    /**
     * Stop recording the media.  <code>stopRecord</code> will not
     * automatically stop the <code>Player</code>.  It only stops
     * the recording.
     * <p>
     * Stopping the <code>Player</code> does not imply
     * a <code>stopRecord</code>.  Rather, the recording
     * will be put into a "standby" mode.  Once the <code>Player</code>
     * is re-started, the recording will resume automatically.
     * <p>
     * After <code>stopRecord</code>, <code>startRecord</code> can
     * be called to resume the recording.
     * <p>
     * If <code>stopRecord</code> is called when the recording has
     * already stopped, it will be ignored.
     * <p>
     * When <code>stopRecord</code> returns, the recording has stopped
     * and a <i>RECORD_STOPPED</i> event will be delivered through the
     * <code>PlayerListener</code>.
     */
    void stopRecord();

    /**
     * Complete the current recording.
     * <p>
     * If the recording is in progress, <code>commit</code>
     * will implicitly call <code>stopRecord</code>.
     * <p>
     * To record again after <code>commit</code> has been called,
     * <code>setRecordLocation</code> or <code>setRecordStream</code>
     * must be called.
     *
     * @exception IOException Thrown if an I/O error occurs during commit.
     * The current recording is not valid. To record again,
     * <code>setRecordLocation</code> or <code>setRecordStream</code>
     * must be called.
     *
     */
    void commit() throws IOException;

    /**
     * Set the record size limit.  This limits the size of the
     * recorded media to the number of bytes specified.
     * <p>
     * When recording is in progress, <code>commit</code> will be
     * called implicitly in the following cases:
     * <ul>
     * <li>
     * Record size limit is reached
     * <li>
     * If the requested size is less than the already recorded size
     * <li>
     * No more space is available.
     * </ul>
     * <p>
     * Once a record size limit has been set, it will remain so
     * for future recordings until it is changed by another
     * <code>setRecordSizeLimit</code> call.
     * <p>
     * To remove the record size limit, set it to
     * <code>Integer.MAX_VALUE</code>.
     * By default, the record size limit is not set.
     * <p>
     * Only positive values can be set.  Zero or negative values
     * are invalid and an <code>IllegalArgumentException</code>
     * will be thrown.
     *
     * @param size The record size limit in number of bytes.
     * @return The actual size limit set.
     * @exception IllegalArgumentException Thrown if the given size
     * is invalid.
     * @exception MediaException Thrown if setting the record
     * size limit is not supported.
     */
    int setRecordSizeLimit(int size) throws MediaException;

    /**
     * Erase the current recording.
     * <p>
     * If the recording is in progress, <code>reset</code>
     * will implicitly call <code>stopRecord</code>.
     * <p>
     * Calling <code>reset</code> after <code>commit</code>
     * will have no effect on the current recording.
     * <p>
     * If the <code>Player</code> that is associated with this
     * <code>RecordControl</code> is closed, <code>reset</code>
     * will be called implicitly.
     *
     * @exception IOException Thrown if the current recording
     * cannot be erased. The current recording is not valid.
     * To record again, <code>setRecordLocation</code> or
     * <code>setRecordStream</code> must be called.
     *
     */
    void reset() throws IOException;
}
