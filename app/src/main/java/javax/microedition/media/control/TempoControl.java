/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;

/**
 * TempoControl controls the tempo, in musical terms, of a song.
 * <p>
 *
 * TempoControl is typically implemented in Players for MIDI
 * media, i.e. playback of a Standard MIDI File (SMF).<p>
 *
 * TempoControl is basic functionality for a MIDI playback
 * application. This is in contrast to {@link MIDIControl MIDIControl},
 * which targets advanced applications. Moreover, TempoControl
 * needs a sequence - e.g. a MIDI file - to operate. MIDIControl
 * does not require a sequence.<p>
 *
 * Musical tempo is usually specified in beats per minute. To
 * provide a means to access tempos with fractional beats per
 * minute, the methods to set and get the tempo work on
 * &quot;milli-beat&quot; per minute. A simple division by
 * 1000 is sufficient to get the actual beats per minute.<p>
 *
 * As a MIDI file can contain any number of tempo changes
 * during playback, the absolute tempo is a state of the
 * sequencer. During playback of a MIDI file, setting the tempo
 * in response to a user interaction will not always yield the
 * desired result: the user's tempo can be overridden by the
 * playing MIDI file to another tempo just moments later.<br>
 * In order to overcome this problem, a relative tempo rate is
 * used (in Java Sound terms: tempo factor). This rate is
 * applied to all tempo settings. The tempo rate is specified
 * in &quot;milli-percent&quot;, i.e. a value of 100'000 means
 * playback at original tempo. The tempo rate is set with the
 * <code>setRate()</code> method of the super class,
 * {@link RateControl RateControl}.<p>
 *
 * The concept of tempo rate allows one to play back a MIDI sequence
 * at a different tempo without losing the relative tempo changes
 * in it.<p>
 *
 * The <code>setTempo()</code> and <code>getTempo()</code> methods
 * do <b>not</b> affect or reflect the playback rate. This means that
 * changing the
 * rate will not result in a change of the value returned by
 * <code>getTempo()</code>. Similarly, setting the tempo with
 * <code>setTempo()</code> does not change the rate, i.e.
 * the return value of <code>getRate()</code> is not changed. The
 * effective playback tempo is always the product of tempo and rate:<br>
 * <br>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
 * effectiveBeatsPerMinute = getTempo()
 * getRate() / 1000 / 100000</code>
 * <p>
 *
 * @see javax.microedition.media.Player
 * @see RateControl
 * @see PitchControl
 * @see MIDIControl
 */

public interface TempoControl extends RateControl {

    /**
     * Sets the current playback tempo.
     *
     * Tempo is a volatile state of the sequencer. As MIDI sequences
     * may contain META tempo events, tempo may change during
     * playback of the sequence. Setting the tempo with
     * <code>setTempo()</code> does not prevent the tempo from
     * being changed subsequently by tempo events in the MIDI
     * sequence. Example: during playback of a sequence,
     * the user changes the tempo. But just moments later, the
     * MIDI sequence changes the tempo to another value, so
     * effectively the user interaction is ignored.
     * To overcome this, and to allow consistent user interaction,
     * use <code>{@link RateControl#setRate(int) setRate()}</code>
     * inherited from <code>RateControl</code>.<p>
     *
     * The <code>setTempo()</code> method returns the actual tempo
     * set by the <code>Player</code>'s implementation. It
     * sets the tempo as close to the requested value as possible,
     * but is not required to set it to the exact value. Specifically,
     * implementations may have a lower or upper limit, which will
     * be used as tempo if the requested tempo is out of limits.
     * 0 or negative tempo does not exist and will always result
     * in the lower tempo limit of the implementation. Implementations
     * are guaranteed to support 10'000 to 300'000 milli-beats per minute.<p>
     *
     * Setting tempo to a stopped sequence will force the
     * sequence to start with that tempo, even if the sequence has a tempo
     * event at the start position. Any subsequent tempo events in
     * the sequence will be considered, though. Rewinding back to
     * a position with a tempo event will result in a
     * tempo change caused by the tempo event, too. Example: a sequence
     * with initial tempo of 120bpm has not been started yet. The
     * user sets the tempo to 140bpm and starts playback. When the
     * playback position is then reset to the beginning, the tempo will be
     * set to 120bpm due to the tempo event at the beginning of the sequence.<p>
     *
     * Playback rate (see <code>{@link RateControl#setRate(int) setRate()}</code>)
     * and tempo are independent factors of the effective tempo. Modifying
     * tempo with <code>setTempo()</code> does not affect the playback
     * rate and vice versa. The effective tempo is the product of tempo and rate.
     *
     * @param millitempo The tempo specified in milli-beats
     *        per minute (must be &gt; 0, e.g. 120'000 for 120 beats per minute)
     * @return tempo that was actually set, expressed in milli-beats per minute
     * @see #getTempo
     */

    int setTempo(int millitempo);

    /**
     * Gets the current playback tempo.
     * This represents the current state of the sequencer:
     * <ul>
     *  <li>A sequencer may not be initialized before the
     *      <code>Player</code> is prefetched.  An uninitialized
     *      sequencer in this case returns
     *      a default tempo of 120 beats per minute.</li>
     *  <li>After prefetching has finished, the tempo is
     *      set to the start tempo of the MIDI sequence (if any).</li>
     *  <li>During playback, the return value is the current tempo and
     *      varies with tempo events in the MIDI file</li>
     *  <li>A stopped sequence retains the last tempo it had
     *      before it was stopped.</li>
     *  <li>A call to <code>setTempo()</code> changes current tempo
     *      until a tempo event in the MIDI file is encountered.</li>
     * </ul>
     * @return current tempo, expressed in milli-beats per minute
     * @see #setTempo
     */
    int getTempo();

}
