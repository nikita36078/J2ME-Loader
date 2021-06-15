/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;

/**
 * <code>VolumeControl</code> is an interface for manipulating
 * the audio volume of a <code>Player</code>.
 *
 * <h3>Volume Settings</h3>
 * This interface allows the output volume to be specified
 * using an integer value that varies between 0 and 100.
 *
 *<h4>Specifying Volume in the Level Scale</h4>
 * The level scale specifies volume in a linear scale.
 * It ranges from 0 to 100, where 0 represents
 * silence and 100 represents the highest volume.
 * The mapping for
 * producing a linear multiplicative value is
 * implementation dependent.
 * <p>
 *
 * <h3>Mute</h3>
 * Setting mute on or off doesn't change the volume level
 * returned by getLevel.
 * If mute is <CODE>true</CODE>,
 * no audio signal is produced by this <code>Player</code>; if mute
 * is <CODE>false</CODE>
 * an audio signal is produced and the volume is restored.
 *
 * <h3>Volume Change Events</h3>
 * When the state of the <code>VolumeControl</code> changes, a
 * <code>VOLUME_CHANGED</code> event is delivered through
 * the <code>PlayerListener</code>.
 *
 * @see javax.microedition.media.Control
 * @see javax.microedition.media.Player
 * @see javax.microedition.media.PlayerListener
 */

public interface VolumeControl extends javax.microedition.media.Control {

    /**
     * Mute or unmute the <code>Player</code> associated with this
     * <code>VolumeControl</code>.
     * <br>
     * Calling <code>setMute(true)</code> on
     * the <code>Player</code> that is already muted is ignored,
     * as is calling <code>setMute(false)</code> on the
     * <code>Player</code> that is not currently muted.
     * Setting mute on or off doesn't change the volume level
     * returned by getLevel.
     * <br>
     * When <code>setMute</code> results in a change in
     * the muted state,
     * a <code>VOLUME_CHANGED</code> event will be delivered
     * through the <code>PlayerListener</code>.
     *
     * @see #isMuted
     *
     * @param mute Specify <CODE>true</CODE> to mute the signal,
     * <CODE>false</CODE> to unmute the signal.
     */
    void setMute(boolean mute);

    /**
     * Get the mute state of the signal associated with this
     * <code>VolumeControl</code>.
     *
     * @see #setMute
     *
     * @return The mute state.
     */
    boolean isMuted();

    /**
     * Set the volume using a linear point scale
     * with values between 0 and 100.
     * <br>
     * 0 is silence; 100 is the loudest
     * useful level that this <code>VolumeControl</code> supports.
     * If the given level is less than 0 or greater than 100,
     * the level will be set to 0 or 100 respectively.
     * <br>
     * When <code>setLevel</code> results in a change in
     * the volume level,
     * a <code>VOLUME_CHANGED</code> event will be delivered
     * through the <code>PlayerListener</code>.
     *
     * @see #getLevel
     *
     * @param level The new volume specified in the level scale.
     * @return The level that was actually set.
     */
    int setLevel(int level);

    /**
     * Get the current volume level set.
     * <br>
     * <code>getLevel</code> may return <code>-1</code>
     * if and only if the <code>Player</code> is in the
     * <i>REALIZED</i> state (the audio device has not been
     * initialized) and <code>setLevel</code> has not
     * yet been called.
     *
     * @see #setLevel
     *
     * @return The current volume level or <code>-1</code>.
     */
    int getLevel();
}
