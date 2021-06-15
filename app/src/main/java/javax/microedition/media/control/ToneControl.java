/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;
//import javax.microedition.media.MediaException;

/**
 * <code>ToneControl</code> is the interface to enable playback of a
 * user-defined monotonic tone sequence.
 * <p>
 * A tone sequence is specified as a list of tone-duration pairs and
 * user-defined sequence blocks.  The list is packaged as an
 * array of bytes.  The <code>setSequence</code> method is used to
 * input the sequence to the <code>ToneControl</code>.  In addition,
 * the tone sequence format specified below can also be used as a
 * file format to define tone sequences.  A file containing a
 * tone sequence as specified must use ".jts" as the file extension.
 * <code>"audio/x-tone-seq"</code> designates the MIME type for this
 * format.
 * <p>
 * <a name="tone_sequence_format"></a>
 * The syntax of a tone sequence is described in
 * <a href="http://www.ietf.org/rfc/rfc2234">Augmented BNF</a> notations:
 * <blockquote>
 * <pre>
 * sequence              = version *1tempo_definition *1resolution_definition
 *		             *block_definition 1*sequence_event
 *
 * version               = VERSION version_number
 * VERSION               = byte-value
 * version_number        = 1	; version # 1
 *
 * tempo_definition      = TEMPO tempo_modifier
 * TEMPO                 = byte-value
 * tempo_modifier        = byte-value
 *              ; multiply by 4 to get the tempo (in bpm) used
 *              ; in the sequence.
 *
 * resolution_definition = RESOLUTION resolution_unit
 * RESOLUTION            = byte-value
 * resolution_unit       = byte-value
 *
 * block_definition      = BLOCK_START block_number
 *                            1*sequence_event
 *                         BLOCK_END block_number
 * BLOCK_START           = byte-value
 * BLOCK_END             = byte-value
 * block_number          = byte-value
 *              ; block_number specified in BLOCK_END has to be the
 *              ; same as the one in BLOCK_START
 *
 * sequence_event        = tone_event / block_event /
 *                           volume_event / repeat_event
 *
 * tone_event            = note duration
 * note                  = byte-value ; note to be played
 * duration              = byte-value ; duration of the note
 *
 * block_event           = PLAY_BLOCK block_number
 * PLAY_BLOCK            = byte-value
 * block_number          = byte-value
 *              ; block_number must be previously defined
 *              ; by a full block_definition
 *
 * volume_event          = SET_VOLUME volume
 * SET_VOLUME            = byte-value
 * volume                = byte-value ; new volume
 *
 * repeat_event          = REPEAT multiplier tone_event
 * REPEAT                = byte-value
 * multiplier            = byte-value
 *              ; number of times to repeat a tone
 *
 * byte-value            = -128 - 127
 *              ; the value of each constant and additional
 *              ; constraints on each parameter are specified below.
 * </pre>
 * </blockquote>
 *
 * <A HREF="#VERSION"><code>VERSION</code></A>,
 * <A HREF="#TEMPO"><code>TEMPO</code></A>,
 * <A HREF="#RESOLUTION"><code>RESOLUTION</code></A>,
 * <A HREF="#BLOCK_START"><code>BLOCK_START</code></A>,
 * <A HREF="#BLOCK_END"><code>BLOCK_END</code></A>,
 * <A HREF="#PLAY_BLOCK"><code>PLAY_BLOCK</code></A>
 * <A HREF="#SET_VOLUME"><code>SET_VOLUME</code></A>
 * <A HREF="#REPEAT"><code>REPEAT</code></A>
 * are pre-defined constants.
 * <p>
 * Following table shows the valid range of the parameters:
 * <blockquote>
 * <table border=1>
 * <tr>
 *   <td align="center">Parameter</td>
 *   <td align="center">Valid Range</td>
 *   <td align="center">Effective Range</td>
 *   <td align="center">Default</td></tr>
 * <tr>
 *   <td><code>tempo_modifier</code></td>
 *   <td align="center">5<= <code>tempo_modifier</code> <= 127 </td>
 *   <td align="center">20bpm to 508bpm</td>
 *   <td align="center">120bpm</td></tr>
 * <tr>
 *   <td><code>resolution_unit</code></td>
 *   <td align="center">1<= <code>resolution_unit</code> <= 127 </td>
 *   <td align="center">1/1 note to 1/127 note</td>
 *   <td align="center">1/64 note</td></tr>
 * <tr>
 *   <td><code>block_number</code></td>
 *   <td align="center">0<= <code>block_number</code> <= 127</td>
 *   <td align="center"> - </td>
 *   <td align="center"> - </td></tr>
 * <tr>
 *   <td><code>note</code></td>
 *   <td align="center">0<= <code>note</code> <= 127 or
 *       <A HREF="#SILENCE"<code>SILENCE</code></a></td>
 *   <td align="center">C-1 to G9 or rest</td>
 *   <td align="center"> - </td></tr>
 * <tr>
 *   <td><code>duration</code></td>
 *   <td align="center">1<= <code>duration</code> <= 127 </td>
 *   <td align="center"> - </td>
 *   <td align="center"> - </td></tr>
 * <tr>
 *   <td><code>volume</code></td>
 *   <td align="center">0<= <code>volume</code> <= 100 </td>
 *   <td align="center">0% to 100% volume</td>
 *   <td align="center">100%</td></tr>
 * <tr>
 *   <td><code>multiplier</code></td>
 *   <td align="center">2<= <code>multiplier</code> <= 127 </td>
 *   <td align="center"> - </td>
 *   <td align="center"> - </td></tr>
 * </table> <br>
 * </blockquote>
 *
 * The frequency
 * of the note can be calculated from the following formula:
 * <pre>
 *     SEMITONE_CONST = 17.31234049066755 = 1/(ln(2^(1/12)))
 *     note = ln(freq/8.176)*SEMITONE_CONST
 * </pre>
 * The musical note A = note 69 (0x45) = 440 Hz. <br>
 * <A HREF="#C4">Middle C (C4)</A> and
 * <A HREF="#SILENCE">SILENCE</A> are defined as constants.
 * <p>
 * The duration of each tone is measured in units of 1/resolution notes
 * and tempo is specified in beats/minute, where 1 beat = 1/4 note.
 * Because the range of positive values of <code>byte</code> is only 1 - 127,
 * the tempo is
 * formed by multiplying the tempo modifier by 4. Very slow tempos
 * are excluded so range of tempo modifiers is 5 - 127 providing an
 * effective range of 20 - 508 bpm.
 * <p>
 * To compute the effective duration in milliseconds for a tone,
 * the following formula can be used:
 * <pre>
 *     duration * 60 * 1000 * 4 / (resolution * tempo)
 * </pre>
 *
 * The following table lists some common durations in musical notes:
 * <blockquote>
 * <table border=1>
 * <tr>
 *   <td align="center">Note Length</td>
 *   <td align="center">Duration, Resolution=64</td>
 *   <td align="center">Duration, Resolution=96</td></tr>
 * <tr>
 *   <td align="center">1/1</td>
 *   <td align="center">64</td>
 *   <td align="center">96</td></tr>
 * <tr>
 *   <td align="center">1/4</td>
 *   <td align="center">16</td>
 *   <td align="center">24</td></tr>
 * <tr>
 *   <td align="center">1/4 dotted</td>
 *   <td align="center">24</td>
 *   <td align="center">36</td></tr>
 * <tr>
 *   <td align="center">1/8</td>
 *   <td align="center">8</td>
 *   <td align="center">12</td></tr>
 * <tr>
 *   <td align="center">1/8 triplets</td>
 *   <td align="center">-</td>
 *   <td align="center">8</td></tr>
 * <tr>
 *   <td align="center">4/1</td>
 *   <td align="center"><code>REPEAT</code> 4 &lt;note&gt; 64</td>
 *   <td align="center"><code>REPEAT</code> 4 &lt;note&gt; 96</td>
 * </table>
 * </blockquote>
 *
 * <h2>Example</h2>
 * <blockquote>
 * <pre>
 *    // "Mary Had A Little Lamb" has "ABAC" structure.
 *    // Use block to repeat "A" section.
 *
 *    byte tempo = 30; // set tempo to 120 bpm
 *    byte d = 8;      // eighth-note
 *
 *    byte C4 = ToneControl.C4;;
 *    byte D4 = (byte)(C4 + 2); // a whole step
 *    byte E4 = (byte)(C4 + 4); // a major third
 *    byte G4 = (byte)(C4 + 7); // a fifth
 *    byte rest = ToneControl.SILENCE; // rest
 *
 *    byte[] mySequence = {
 *        ToneControl.VERSION, 1,   // version 1
 *        ToneControl.TEMPO, tempo, // set tempo
 *        ToneControl.BLOCK_START, 0,   // start define "A" section
 *        E4,d, D4,d, C4,d, E4,d,       // content of "A" section
 *        E4,d, E4,d, E4,d, rest,d,
 *        ToneControl.BLOCK_END, 0,     // end define "A" section
 *        ToneControl.PLAY_BLOCK, 0,    // play "A" section
 *        D4,d, D4,d, D4,d, rest,d,     // play "B" section
 *        E4,d, G4,d, G4,d, rest,d,
 *        ToneControl.PLAY_BLOCK, 0,    // repeat "A" section
 *        D4,d, D4,d, E4,d, D4,d, C4,d  // play "C" section
 *    };
 *
 *    try{
 *        Player p = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
 *        p.realize();
 *        ToneControl c = (ToneControl)p.getControl("ToneControl");
 *        c.setSequence(mySequence);
 *        p.start();
 *    } catch (IOException ioe) {
 *    } catch (MediaException me) { }
 *
 * </pre>
 * </blockquote>
 *
 */
public interface ToneControl extends javax.microedition.media.Control {

    /**
     * The VERSION attribute tag.
     * <p>
     * Value -2 is assigned to <code>VERSION</code>.
     */
    byte VERSION = -2;

    /**
     * The TEMPO event tag.
     * <p>
     * Value -3 is assigned to <code>TEMPO</code>.
     */
    byte TEMPO = -3;

    /**
     * The RESOLUTION event tag.
     * <p>
     * Value -4 is assigned to <code>RESOLUTION</code>.
     */
    byte RESOLUTION = -4;

    /**
     * Defines a starting point for a block.
     * <p>
     * Value -5 is assigned to <code>BLOCK_START</code>.
     */
    byte BLOCK_START = -5;

    /**
     * Defines an ending point for a block.
     * <p>
     * Value -6 is assigned to <code>BLOCK_END</code>.
     */
    byte BLOCK_END = -6;

    /**
     * Play a defined block.
     * <p>
     * Value -7 is assigned to <code>PLAY_BLOCK</code>.
     */
    byte PLAY_BLOCK = -7;

    /**
     * The SET_VOLUME event tag.
     * <p>
     * Value -8 is assigned to <code>SET_VOLUME</code>.
     */
    byte SET_VOLUME = -8;

    /**
     * The REPEAT event tag.
     * <p>
     * Value -9 is assigned to <code>REPEAT</code>.
     */
    byte REPEAT = -9;

    /**
     * Middle C.
     * <p>
     * Value 60 is assigned to <code>C4</code>.
     */
    byte C4 = 60;

    /**
     * Silence.
     * <p>
     * Value -1 is assigned to <code>SILENCE</code>.
     */
    byte SILENCE = -1;

    /**
     * Sets the tone sequence.<p>
     *
     * @param sequence The sequence to set.
     * @exception IllegalArgumentException Thrown if the sequence is
     * <code>null</code> or invalid.
     * @exception IllegalStateException Thrown if the <code>Player</code>
     * that this control belongs to is in the <i>PREFETCHED</i> or
     * <i>STARTED</i> state.
     */
    void setSequence(byte[] sequence);
}
