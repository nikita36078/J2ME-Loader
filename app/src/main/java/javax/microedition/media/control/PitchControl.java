/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;

/**

 * PitchControl raises or lowers the playback pitch of audio without
 * changing the playback speed.
 * <p>
 *
 * PitchControl can be implemented in Players for MIDI media or
 * sampled audio. It is not possible to set audible output to
 * an absolute pitch value. This control raises or lowers pitch
 * relative to the original.<p>
 *
 * The pitch change is specified in number of &quot;milli-
 * semitones&quot; to raise the pitch. As an example,
 * specifying a pitch of 12'000 results in playback one octave
 * higher. For MIDI that means that all MIDI notes are raised
 * by 12 (semitones). For sampled audio playback, it means doubling the
 * frequency of perceived sounds (i.e. a 440Hz sound will become a 880Hz
 * sound.).
 * Negative values are used to lower the pitch.
 * <p>
 * All <code>Players</code> by default support 0, or no pitch change.
 * A <code>Player</code> which supports only 0 pitch change
 * must not implement <code>PitchControl</code>.
 * <p>
 *
 * PitchControl does not influence playback volume in any way.
 *
 * @see javax.microedition.media.Player
 * @see RateControl
 * @see TempoControl
 */

public interface PitchControl extends javax.microedition.media.Control {

    /**
     * Sets the relative pitch raise.
     *
     * The pitch change is specified in &quot;milli-
     * semitones&quot;, i.e. 1000 times the number of
     * semitones to raise the pitch. Negative values
     * lower the pitch by the number of milli-semitones.<p>
     *
     * The <code>setPitch()</code> method returns the actual pitch
     * change set by the {@link javax.microedition.media.Player Player}.
     * <code>Players</code>
     * should set their pitch raise as close to the requested value
     * as possible, but are not required to set it to the exact
     * value of any argument other than 0. A <code>Player</code> is
     * only guaranteed to set its pitch change exactly to 0.
     * If the given pitch raise is less than the value returned by
     * <code>getMinPitch</code>
     * or greater than the value returned by <code>getMaxPitch</code>,
     * it will be adjusted to the minimum or maximum
     * supported pitch raise respectively.
     *
     * @param millisemitones The number of semi tones to raise the playback pitch.
     *        It is specified in &quot;milli-semitones&quot;.
     * @return The actual pitch raise set in &quot;milli-semitones&quot;.
     * @see #getPitch
     */
    int setPitch(int millisemitones);

    /**
     * Gets the current playback pitch raise.
     *
     * @return the current playback pitch raise in &quot;milli-semitones&quot;.
     * @see #setPitch
     */
    int getPitch();

    /**
     * Gets the maximum playback pitch raise supported by the <code>Player</code>.
     *
     * @return the maximum pitch raise in &quot;milli-semitones&quot;.
     */
    int getMaxPitch();

    /**
     * Gets the minimum playback pitch raise supported by the <code>Player</code>.
     *
     * @return the minimum pitch raise in &quot;milli-semitones&quot;.
     */
    int getMinPitch();

}
