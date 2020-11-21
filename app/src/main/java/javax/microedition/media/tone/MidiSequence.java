/*
 * Copyright (c) 2002 Nokia Corporation and/or its subsidiary(-ies).
 * All rights reserved.
 * This component and the accompanying materials are made available
 * under the terms of "Eclipse Public License v1.0"
 * which accompanies this distribution, and is available
 * at the URL "http://www.eclipse.org/legal/epl-v10.html".
 *
 * Initial Contributors:
 * Nokia Corporation - initial contribution.
 *
 * Contributors:
 *
 * Description:  This class represents midi sequence
 *
 */

package javax.microedition.media.tone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class represents midi sequence
 */
public class MidiSequence {
	/* MIDI events track stream granularity */
	private static final int MIDI_EVENTS_TRACK_GRANULARITY = 100;

	// MIDI constants

	/* Note value used for silence events. This will not be audible */
	public static final byte MIDI_SILENCE_NOTE = 0;

	// MIDI file constants

    /* Maximum length of midi sequence events. After this has been
       met, no new events are accepted by writeMidiEvent. This is not
       a hard limit anyway (may be exceeded by the last written event),
       so it's only here to guard memory use and possible infinite
       sequence */

	private static final int MIDI_EVENTS_MAX_BYTE_COUNT = 32768;

	// MIDI track constants

	/* Length of single midi event without the variable length
	   delta time */
	private static final int MIDI_EVENT_COMMAND_LENGTH = 3;

	/* Channel mask for setting correct channel in writeMidiEvent */
	private static final byte MIDI_EVENT_CHANNEL_MASK = (byte) 0xF0;

	/* Maximum value for midi variable length quantity */
	private static final int MIDI_VARIABLE_LENGTH_MAX_VALUE = 0x0FFFFFFF;

	// Tone constants

	/* Tone resolution is expressed in pulses per full note, whereas
	   midi resolution is pulses per quarter note. Thus we must divide tone
	   empo by 4. For tone, 64 is considered default */
	private static final byte TONE_DEFAULT_RESOLUTION = 64; // 64/4 = 16(ppqn)

	/* Default tempo for tone is 30. For bpm value it is multiplied by 4 */
	private static final byte TONE_DEFAULT_TEMPO = 30; // 4*30 = 120 (bpm)

	/* Tone multiplier is used for both dividing resolution and multiplying
	   tempo to get equivalent midi values */
	private static final byte TONE_MULTIPLIER = 1;

	/* Midi channel for generated MIDI sequence */
	private byte channel;

	/* Tempo in MIDI terms */
	private int tempo;

	/* Resolution in MIDI terms */
	private int resolution;

	/* Instrument used to represent tone */
	private byte instrument;

	/* Counter for written midi events */
	private int midiEventsByteCount;

	/* MIDI sequence written using writeEvent( ) */
	private ByteArrayOutputStream midiTrackEvents;

	/* Tone sequence duration */
	private int duration;

	/**
	 * Constructor
	 *
	 * @param channel    MIDI channel which is assigned to generate track
	 * @param instrument Instrument used to represent tone
	 */
	MidiSequence(byte channel, byte instrument) {
		this.channel = channel;
		this.instrument = instrument;
		tempo = TONE_DEFAULT_TEMPO * TONE_MULTIPLIER;
		resolution = TONE_DEFAULT_RESOLUTION / TONE_MULTIPLIER;
		midiTrackEvents = new ByteArrayOutputStream(MIDI_EVENTS_TRACK_GRANULARITY);
	}

	/**
	 * Get midi stream
	 */
	public ByteArrayInputStream getStream() throws IOException {
		midiTrackEvents.flush();
		byte[] midiTrackEvents = this.midiTrackEvents.toByteArray();
		ByteArrayOutputStream concateStream = new ByteArrayOutputStream(midiTrackEvents.length);
		concateStream.write(midiTrackEvents);
		ByteArrayInputStream midi = new ByteArrayInputStream(concateStream.toByteArray());

		concateStream.close();
		return midi;
	}

	/**
	 * Get midi file data as byte[]
	 */
	public byte[] getByteArray() throws IOException {
		midiTrackEvents.flush();
		byte[] midiTrackEvents = this.midiTrackEvents.toByteArray();
		ByteArrayOutputStream concateStream = new ByteArrayOutputStream(midiTrackEvents.length);
		concateStream.write(midiTrackEvents);

		byte[] midi = concateStream.toByteArray();
		concateStream.close();
		return midi;
	}

	/**
	 * Set tempo
	 *
	 * @param tempo tempo in tone sequence terms
	 */
	public void setTempo(int tempo) {
		if (tempo < MidiToneConstants.TONE_TEMPO_MIN || tempo > MidiToneConstants.TONE_TEMPO_MAX) {
			throw new IllegalArgumentException("Tempo is out of range, " +
					"valid range is 5 <= tempo <= 127");
		}
		this.tempo = tempo * TONE_MULTIPLIER;
	}

	/**
	 * Set resolution
	 *
	 * @param resolution resolution in tone sequence terms
	 */
	public void setResolution(int resolution) {
		if (resolution < MidiToneConstants.TONE_RESOLUTION_MIN ||
				resolution > MidiToneConstants.TONE_RESOLUTION_MAX) {
			throw new IllegalArgumentException("Resolution is out of range, " +
					"valid range is 1 <= resolution <= 127");
		}
		this.resolution = resolution / TONE_MULTIPLIER;
	}

	/*
	 * Write midi event to stream. This method writes both variable length
	 * delta time and midi event.
	 * @param length time between last event and this event (delta time)
	 * @param command MIDI command byte
	 * @param event First MIDI command parameter
	 * @param data Second MIDI command parameter
	 */
	public void writeMidiEvent(int length, byte command, byte event, byte data)
			throws MidiSequenceException {
		if (midiEventsByteCount > MIDI_EVENTS_MAX_BYTE_COUNT) {
			throw new MidiSequenceException();
		}
		midiEventsByteCount += writeVarLen(length);

		// Write down cumulative count of event lengths (sum will
		// make up duration of this midi sequence. Only audible events
		// are counted, which means only those delta times which
		// are associated to NOTE_OFF events
		if (command == MidiToneConstants.MIDI_NOTE_OFF) {
			duration += length;
		}

		// attach correct channel number
		command &= MIDI_EVENT_CHANNEL_MASK;
		command |= channel;

		midiTrackEvents.write(command);
		midiTrackEvents.write(event);
		midiTrackEvents.write(data);
		midiEventsByteCount += MIDI_EVENT_COMMAND_LENGTH;
	}

	/**
	 * Write time interval value
	 *
	 * @param value time before the event in question happens, relative to
	 *              current time. Must be between 0 and 0x0FFFFFFF
	 */
	private int writeVarLen(int value) {
		if ((value > MIDI_VARIABLE_LENGTH_MAX_VALUE) || (value < 0)) {
			throw new IllegalArgumentException("Input(time) value is not within range");
		}

		// Variable to hold count of bytes written to output stream.
		// Value range is 1-4.
		int byteCount = 0;

		// variable length quantity can any hold unsigned integer value which
		// can be represented with 7-28 bits. It is written out so that 7 low
		// bytes of each byte hold part of the value and 8th byte indicates
		// whether it is last byte or not (0 if is, 1 if not). Thus a variable
		// length quantity can be 1-4 bytes long.

		int buffer = value & 0x7F; // put low 7 bytes to buffer

		// check if bits above 7 first are significant, 7 bits at time. If
		// they are, buffer is shifted 8 bits left and the new 7 bits are
		// appended to beginning of buffer. The eigth byte from right is
		// set 1 to indicate that that there is at least another 7 bits
		// on left (bits 9-15) which are part of the quantity.

		// Example. Integer 00000100 11111010 10101010 01010101
		// 1) Set low 7 bytes to buffer => 1010101
		// 2) Check if there is more significant bytes in the integer. If
		// is, continue.
		// 3) Shift buffer 8 left => 1010101 00000000
		// 4) Append next 7 bytes to beginning of buffer
		// buffer => 1010101 01010100
		// 5) Set 8th bit 1 to indicate that there is another 7 bits on left
		// buffer => 1010101 11010100
		// 6) repeat from step 2

		value >>= 7;
		while (value != 0) {
			buffer <<= 8;
			buffer |= ((value & 0x7F) | 0x80);
			value >>= 7;
		}

		// write the buffer out as 1-4 bytes.
		while (true) {
			byteCount++;

			// check if the indicator bit (8th) is set.
			// If it is, continue writing.
			int tempBuf = buffer & 0x80;
			if (tempBuf != 0) {
				buffer >>= 8;
			} else {
				break;
			}
		}
		return byteCount;
	}

	/**
	 * Return duration accumulated so far.
	 *
	 * @return long duration in microseconds
	 */
	public long getCumulativeDuration() {
		// duration * seconds in minute * microseconds in second /
		// (resolution * tempo)
		long duration = (long) this.duration * 60 * 1000000 / (resolution * tempo);
		return duration;
	}
}
