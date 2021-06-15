/*
 * %W% %E%
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.microedition.media.control;

import javax.microedition.media.MediaException;


/**
 * <code>MIDIControl</code> provides access to MIDI rendering
 * and transmitting devices.<p>
 *
 * Typical devices that are controlled with <code>MIDIControl</code>
 * are internal synthesizers (software/hardware) or external
 * hardware ports. Devices are virtual, i.e. even if there is only
 * one physical synthesizer, all instances of <code>MIDIControl</code> seem
 * to operate on its own synthesizer.<p>
 *
 * General functionality of this control is:
 * <ol>
 *  <li>Querying current state of the device:
 *  <ul>
 *   <li>The programs that are currently assigned to each of the 16 channels</li>
 *   <li>Volume of each channel</li>
 *  </ul></li>
 *  <li>Querying the banks of the synthesizer:
 *  <ul>
 *   <li>Get a list of internal sound banks</li>
 *   <li>Get a list of custom sound banks</li>
 *   <li>Get the list of programs of a sound bank</li>
 *   <li>Get the name of a specific program</li>
 *  </ul></li>
 *  <li>Set the volume assigned to a channel</li>
 *  <li>Set the bank/program assigned to a channel</li>
 *  <li>Send short MIDI messages to the device</li>
 *  <li>Send long MIDI messages (system exclusive)</li>
 * </ol>
 *
 * In Java Sound terms, <code>MIDIControl</code> combines
 * methods and concepts of the interfaces Transmitter,
 * Receiver, Synthesizer, MidiChannel, Soundbank, and Patch.<p>
 *
 * In this context, the following naming conventions are used:
 * <ul>
 *  <li>A <i>program</i> refers to a single instrument. This is
 *  also known as a patch.</li>
 *  <li>A <i>bank</i> is short for sound bank. It contains up
 *  to 128 programs, numbered in the range from 0..127.</li>
 *  <li>An <i>internal bank</i> is provided by the software
 *  implementation or the hardware of the device.</li>
 *  <li>A <i>custom bank</i> is installed by an application,
 *  e.g. by loading an XMF meta file with an embedded bank.</li>
 * </ul>
 * <p>
 * The conception of <code>MIDIControl</code> is based on scope and
 * abstraction level:
 * <ul>
 *  <li><code>MIDIControl</code> has methods that are specific
 *  to the device or renderer, and do not directly relate to a specific
 *  MIDI file or sequence to be played. However, as devices are virtual,
 *  MIDIControl's methods only operate on this virtual device.
 *  On the other hand, it is also
 *  possible to get an instance of <code>MIDIControl</code>
 *  without providing a sequence or MIDI file; this is done by
 *  specifying a magic Locator:<br>
 *  <br><code>
 *  &nbsp;&nbsp;try{
 *  <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Player
 *    p = Manager.createPlayer(Manager.MIDI_DEVICE_LOCATOR);
 *  <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;MIDIControl
 *    synth = (MIDIControl)p.getControls("javax.microedition.media.control.MIDIControl");
 *  <br>&nbsp;&nbsp;} catch (MediaException e) {
 *  <br>&nbsp;&nbsp;}
 *  </code></li>
 *
 *  <li><code>MIDIControl</code>'s methods can be considered
 *  advanced, low level functionality. This has 2 implications:
 *  <ol>
 *   <li><code>MIDIControl</code> is optional, i.e. no Player
 *   instance is required to provide an implementation of
 *   it</li>
 *   <li>Basic media or MIDI player applications will not need
 *   <code>MIDIControl</code>; {@link VolumeControl VolumeControl},
 *   {@link TempoControl TempoControl}, and {@link PitchControl PitchControl}
 *   are sufficient for basic needs.
 *   </li>
 *  </ol></li>
 * </ul>
 * <p>
 * A useful function is &quot;Panic&quot;: immediately turn off all
 * sounds and notes. It can be implemented using the following code fragment:<br>
 * <code>
 * &nbsp;&nbsp;int CONTROL_ALL_SOUND_OFF = 0x78;<br>
 * &nbsp;&nbsp;for (int channel = 0; channel < 16; channel++) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_ALL_SOUND_OFF, 0);<br>
 * &nbsp;&nbsp;}<br>
 * </code>
 * <p>
 * The implementation need not support the various query methods.
 * This is a technical limitation, as the MIDI standard does not
 * provide a standardized means to query the current program or
 * the installed
 * soundbanks. This especially applies to external MIDI ports.
 * Optional methods must not be called if {@link #isBankQuerySupported isBankQuerySupported}
 * returns false.
 *
 * @see javax.microedition.media.Player
 * @see RateControl
 * @see TempoControl
 * @see PitchControl
 */

public interface MIDIControl extends javax.microedition.media.Control {

    // constants for MIDI status (upper nibble of first byte)

    /**
     * Command value for Note On message (0x90, or 144).
     * To turn a note off, send a NOTE_ON message with 0
     * velocity. Alternatively, a Note Off message (0x80)
     * can be sent.
     *
     * @see #shortMidiEvent(int, int, int)
     */
    int NOTE_ON = 0x90;  // 144


    /**
     * Command value for Control Change message (0xB0, or 176).
     * @see #shortMidiEvent(int, int, int)
     */
    int CONTROL_CHANGE = 0xB0;  // 176

    // query device state

    /**
     * Returns whether banks of the synthesizer can be queried.
     * <p>
     * If this functions returns true,
     * then the following methods can be used to query banks:
     * <ul>
     * <li>{@link #getProgram(int) getProgram(int)}</li>
     * <li>{@link #getBankList(boolean) getBankList(boolean)}</li>
     * <li>{@link #getProgramList(int) getProgramList(int)}</li>
     * <li>{@link #getProgramName(int, int) getProgramName(int, int)}</li>
     * <li>{@link #getKeyName(int, int, int) getKeyName(int, int, int)}</li>
     * </ul>
     *
     * @return true if this device supports querying of banks
     */
    boolean isBankQuerySupported();


    // send a Program Change short MIDI message, or
    /**
     * Returns program assigned to channel. It represents the current
     * state of the channel. During playback of a MIDI file, the program
     * may change due to program change events in the MIDI file.<p>
     * To set a program for a channel,
     * use setProgram(int, int, int).<p>
     *
     * The returned array is represented by an array {bank,program}.<p>
     * If the device has not been initialized with a MIDI file, or the MIDI file
     * does not contain a program change for this channel, an implementation
     * specific default value is returned.<p>
     *
     * As there is no MIDI equivalent to this method, this method is
     * optional, indicated by {@link #isBankQuerySupported isBankQuerySupported}.
     * If it returns false, this function is not supported and throws an exception.
     *
     * @param channel 0-15
     * @return program assigned to channel, represented by array {bank,program}.
     * @exception IllegalArgumentException Thrown if <code>channel</code>
     * is out of range.
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @exception MediaException Thrown if querying of banks is not supported.
     * @see #isBankQuerySupported
     * @see #setProgram
     */
    int[] getProgram(int channel)
	throws MediaException;


    /**
     * Get volume for the given channel. The return value is
     * independent of the master volume, which is set and retrieved
     * with {@link VolumeControl VolumeControl}.<p>
     *
     * As there is no MIDI equivalent to this method, the implementation
     * may not always know the current volume for a given channel. In
     * this case the return value is -1.
     *
     * @param channel 0-15
     * @return channel volume, 0-127, or -1 if not known
     * @exception IllegalArgumentException Thrown if <code>channel</code>
     * is out of range.
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @see #setChannelVolume(int, int)
     */
    int getChannelVolume(int channel);


    // set device state

    /**
     * Set program of a channel. This sets the current program for the
     * channel and may be overwritten during playback by events in a MIDI sequence.<p>
     * It is a high level convenience function. Internally, these method calls are
     * executed:<p>
     * <code>
     * &nbsp;&nbsp;shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_BANK_CHANGE_MSB, bank >> 7);<br>
     * &nbsp;&nbsp;shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_BANK_CHANGE_LSB, bank & 0x7F);<br>
     * &nbsp;&nbsp;shortMidiEvent(PROGRAM_CHANGE | channel, program, 0);
     * </code><p>
     *
     * In order to use the default bank (the initial bank), set the bank parameter to -1.
     * <p>
     *
     * In order to set a program without explicitly setting the bank,
     * use the following call: <p>
     * <code>
     * &nbsp;&nbsp;shortMidiEvent(PROGRAM_CHANGE | channel, program, 0);
     * </code><p>
     *
     * In both examples, the following constants are used:<p>
     * <code>
     * &nbsp;&nbsp;int PROGRAM_CHANGE = 0xC0;<br>
     * &nbsp;&nbsp;int CONTROL_BANK_CHANGE_MSB = 0x00;<br>
     * &nbsp;&nbsp;int CONTROL_BANK_CHANGE_LSB = 0x20;
     * </code><p>
     *
     * @param channel 0-15
     * @param bank 0-16383, or -1 for default bank
     * @param program 0-127
     * @exception IllegalArgumentException Thrown if any of the given
     * parameters is out of range.
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @see #getProgram
     */
    void setProgram(int channel, int bank, int program);


    /**
     * Set volume for the given channel. To mute, set to 0.
     * This sets the current volume for the
     * channel and may be overwritten during playback by events in a MIDI sequence.<p>
     * It is a high level convenience function. Internally, the following command
     * is executed:<p>
     * <code>
     * &nbsp;&nbsp;shortMidiEvent(CONTROL_CHANGE | channel, CONTROL_MAIN_VOLUME, 0);
     * </code><p>
     * where this constant is used:<p>
     * <code>&nbsp;&nbsp;int CONTROL_MAIN_VOLUME = 0x07</code><p>
     *
     * The channel volume is independent of the master volume, which
     * is accessed with {@link VolumeControl VolumeControl}.
     * Setting the channel volume does not modify the value of the master
     * volume - and vice versa: changing the value of master volume does not
     * change any channel's volume value.<br>
     * The synthesizer
     * mixes the output of up to 16 channels, each channel with its own
     * channel volume. The master volume then controls the volume of the mix.
     * Consequently, the effective output volume of a channel is the product
     * of master volume and channel volume. <p>
     *
     * Setting the channel volume does not generate a
     * {@link javax.microedition.media.PlayerListener#VOLUME_CHANGED VOLUME_CHANGED event}.
     *
     * @param channel 0-15
     * @param volume 0-127
     * @exception IllegalArgumentException Thrown if
     *            <code>channel</code> or <code>volume</code> is out of range.
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @see #getChannelVolume
     */
    void setChannelVolume(int channel, int volume);


    // banks

    /**
     * Returns list of installed banks.
     * If the <code>custom</code> parameter is true, a list of custom banks is returned.
     * Otherwise, a list of all banks (custom and internal) is returned.
     * <p>
     * As there is no MIDI equivalent to this method, this method is
     * optional, indicated by {@link #isBankQuerySupported isBankQuerySupported}.
     * If it returns false, this function is not supported and throws an exception.
     *
     * @param custom if set to true, returns list of custom banks.
     * @return an array of all installed bank numbers.
     *         Each bank number is in the range of 0..16383
     * @exception MediaException if this device does not support retrieval of
     *            banks
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @see #isBankQuerySupported
     */
    int[] getBankList(boolean custom)
	throws MediaException;


    /**
     * Given bank, get list of program numbers. If and only if
     * this bank is not installed, an empty array is returned.<p>
     *
     * As there is no MIDI equivalent to this method, this method is
     * optional, indicated by {@link #isBankQuerySupported isBankQuerySupported}.
     * If it returns false, this function is not supported and throws an exception.
     *
     * @param bank 0..16383
     * @return an array of programs defined in the given bank.
     *         Each program number is from 0..127.
     * @exception IllegalArgumentException Thrown if <code>bank</code>
     *            is out of range.
     * @exception MediaException Thrown if the device does not support
     *            retrieval of programs.
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @see #setProgram
     * @see #isBankQuerySupported
     */
    int[] getProgramList(int bank)
	throws MediaException;


    /**
     * Given bank and program, get name of program.
     * For space-saving reasons, an implementation may return an empty string.
     * <p>
     * As there is no MIDI equivalent to this method, this method is
     * optional, indicated by {@link #isBankQuerySupported isBankQuerySupported}.
     * If it returns false, this function is not supported and throws an exception.
     *
     * @param bank 0-16383
     * @param prog 0-127
     * @exception IllegalArgumentException Thrown if <code>bank</code>
     *            or <code>prog</code> is out of range.
     * @exception MediaException Thrown if the bank or program is
     *            not installed (internal or custom), or if this device does not
     *            support retrieval of program names
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @return name of the specified program, or empty string.
     * @see #isBankQuerySupported
     */
    String getProgramName(int bank, int prog)
	throws MediaException;


    /**
     * Given bank, program and key, get name of key.
     * This method applies to key-mapped banks (i.e. percussive banks
     * or effect banks) only.
     * A return value of <code>null</code> means that the specified key
     * is not mapped to a sound. For melodic banks,
     * where each key (=note) produces the same sound at different pitch, this method
     * always returns <code>null</code>.
     * For space-saving reasons, an implementation may return an empty string
     * instead of the key name. To find out which keys in a specific program
     * are mapped to a sound, iterate through all keys (0-127) and compare
     * the return value of <code>getKeyName</code> to non-<code>null</code>.
     * <p>
     * As there is no MIDI equivalent to this method, this method is
     * optional, indicated by {@link #isBankQuerySupported isBankQuerySupported}.
     * If it returns false, this function is not supported and throws an exception.
     *
     * @param bank 0-16383
     * @param prog 0-127
     * @param key 0-127
     * @exception IllegalArgumentException Thrown if <code>bank</code>,
     *            <code>prog</code> or <code>key</code> is out of range.
     * @exception MediaException Thrown if the bank or program is
     *            not installed (internal or custom), or if this device does not
     *            support retrieval of key names
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @return name of the specified key, empty string, or <code>null</code> if
     *            the key is not mapped to a sound.
     * @see #isBankQuerySupported
     */
    String getKeyName(int bank, int prog, int key)
	throws MediaException;


    /**
     * Sends a short MIDI event to the device.
     * Short MIDI events consist of 1, 2, or 3 unsigned bytes.
     * For non-realtime events, the first byte is split up into
     * status (upper nibble, 0x80-0xF0) and channel (0x00-0x0F).
     * For example, to send a <code>Note On</code> event on a given channel,
     * use this line:<p>
     * <code>&nbsp;&nbsp;shortMidiEvent(NOTE_ON | channel, note, velocity);</code><p>
     * For events with less than 3 bytes, set the remaining data bytes to 0.<p>
     *
     * There is no guarantee that a specific
     * implementation of a MIDI device supports all event types.
     * Also, the MIDI protocol does not implement flow control and it is not
     * guaranteed that an event reaches the destination.
     * In both these cases, this method fails silently. <p>
     *
     * Static error checking is performed on the passed parameters. They have to
     * specify a valid, complete MIDI event. Events with <code>type</code> &lt; 0x80 are
     * not valid MIDI events (-&gt; running status). When an invalid event
     * is encountered, an IllegalArgumentException is thrown.
     *
     * @param type 0x80..0xFF, excluding 0xF0 and 0xF7, which are reserved for system exclusive
     * @param data1 for 2 and 3-byte events: first data byte, 0..127
     * @param data2 for 3-byte events: second data byte, 0..127
     * @exception IllegalArgumentException Thrown if one of the parameters
     *            is out of range.
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     */
    void shortMidiEvent(int type, int data1, int data2);


    /**
     * Sends a long MIDI event to the device, typically a system exclusive message.
     * This method passes the data directly to the receiving device.
     * The data array's contents are not checked for validity.<p>
     * It is possible to send short events, or even a series of short events
     * with this method.<p>
     *
     * @param data array of the bytes to send
     * @param offset start offset in data array
     * @param length number of bytes to be sent
     * @exception IllegalArgumentException Thrown if any one of the given
     *            parameters is not valid.
     * @exception IllegalStateException Thrown if the player has not been prefetched.
     * @return the number of bytes actually sent to the device or
     *         -1 if an error occurred
     */
    int longMidiEvent(byte[] data, int offset, int length);
}
