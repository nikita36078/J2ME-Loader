/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;

/**
 * The <code>FramePositioningControl</code> is the interface to control
 * precise positioning to a video frame for <code>Players</code>.
 * <p>
 * Frame numbers for a bounded movie must be non-negative
 * and should generally begin with 0,
 * corresponding to media time 0.  Each video frame of a movie
 * must have a unique frame number that is one bigger than the
 * previous frame.
 * <p>
 * There is a direct mapping between the frame number and the media
 * time of a video frame; although not all <code>Players</code> can
 * compute that relationship.  For <code>Players</code> that can
 * compute that relationship, the <code>mapFrameToTime</code> and
 * <code>mapTimeToFrame</code> methods can be used.
 * <p>
 * When a <code>Player</code> is seeked or skipped to a new video frame,
 * the media time of the <code>Player</code> will be changed to the
 * media time of the corresponding video frame.
 * <p>
 * As much as possible, the methods in this interface should
 * provide frame-level accuracy with a plus-or-minus-one-frame
 * margin of error to accommodate for round-off errors.
 * However, if the content has inaccurate frame positioning
 * information, implementations may not be able to provide
 * the necessary frame-level accuracy.  For instance, some
 * media content may contain wrong time-stamps or have missing
 * frames.  In any case, the results of each
 * operation should represent the best effort.  For the
 * <code>seek</code> and <code>skip</code> methods, the returned
 * value should indicate the actual new location or the number
 * of frames skipped.
 *
 */
public interface FramePositioningControl extends javax.microedition.media.Control {

    /**
     * Seek to a given video frame.
     * The media time of the <code>Player</code> will be updated
     * to reflect the new position set.
     * <p>
     * This method can be called on a stopped or started
     * <code>Player</code>.
     * If the <code>Player</code> is
     * in the <i>Started</i> state, this method may cause the
     * <code>Player</code> to change states.  If that happens, the
     * appropriate transition events will be posted by
     * the <code>Player</code> when its state changes.
     * <p>
     * If the given frame number is less than the first or larger
     * than the last frame number in the media, <code>seek</code>
     * will jump to either the first or the last frame respectively.
     *
     * @param frameNumber the frame to seek to.
     * @return the actual frame that the Player has seeked to.
     */
    int seek(int frameNumber);

    /**
     * Skip a given number of frames from the current position.
     * The media time of the <code>Player</code> will be updated to
     * reflect the new position set.
     * <p>
     * This method can be called on a stopped or started <code>Player</code>.
     * If the <code>Player</code> is in the <i>Started</i> state,
     * the current position is changing.  Hence,
     * the frame actually skipped to will not be exact.
     * <p>
     * If the <code>Player</code> is
     * in the <i>Started</i> state, this method may cause the
     * <code>Player</code> to change states.  If that happens, the
     * appropriate transition events will be posted.
     * <p>
     * If the given <code>framesToSkip</code> will cause the position to
     * extend beyond the first or last frame, <code>skip</code> will
     * jump to the first or last frame respectively.
     *
     * @param framesToSkip the number of frames to skip from the current
     *   position.  If framesToSkip is positive, it will seek forward
     *   by framesToSkip number of frames.  If framesToSkip is negative,
     *   it will seek backward by framesToSkip number of frames.
     *   e.g. skip(-1) will seek backward one frame.
     * @return the actual number of frames skipped.
     */
    int skip(int framesToSkip);

    /**
     * Converts the given frame number to the corresponding media time.
     * The method only performs the calculations.  It does not
     * position the media to the given frame.
     *
     * @param frameNumber the input frame number for the conversion.
     * @return the converted media time in microseconds for the given frame.
     * If the conversion fails, -1 is returned.
     */
    long mapFrameToTime(int frameNumber);

    /**
     * Converts the given media time to the corresponding frame number.
     * The method only performs the calculations.  It does not
     * position the media to the given media time.
     * <p>
     * The frame returned is the nearest frame that has a media time
     * less than or equal to the given media time.
     * <p>
     * <code>mapTimeToFrame(0)</code> must not fail and must
     * return the frame number of the first frame.
     *
     * @param mediaTime the input media time for the
     * conversion in microseconds.
     * @return the converted frame number for the given media time.
     * If the conversion fails, -1 is returned.
     */
    int mapTimeToFrame(long mediaTime);
}


