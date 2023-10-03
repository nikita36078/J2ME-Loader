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

	/* Value of minute expressed as microseconds */
	private static final int MINUTE_AS_MICROSECONDS = 60000000;

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

	/* Length of MIDI file header. This includes following:
	   MThd block id, MThd length, midi format, MTrk chunk amount and PPQN */
	private static final byte FILE_HEADER_LENGTH = 14;

	/* Length of MTrk block. This includes: MTrk block id, MTrk length */
	private static final byte MTRK_HEADER_LENGTH = 8;

	/* Length of MIDI track header. This includes:
	   tempo change, tempo value, program change */
	private static final int TRACK_HEADER_LENGTH = 10;

	/* Length of MIDI track trailer */
	private static final int TRACK_TRAILER_LENGTH = 4;

	// MIDI file header constants

	/* Header block for MThd */
	private static final byte[] MIDI_HEADER_MTHD = {0x4D, 0x54, 0x68, 0x64};

	/* Header block for MThd block length; value is 6 */
	private static final byte[] MIDI_HEADER_MTHD_LENGTH = {0x00, 0x00, 0x00, 0x06};

	/* Header block for used MIDI format; format is 0 */
	private static final byte[] MIDI_HEADER_MIDI_FORMAT = {0x00, 0x00};

	/* Header block for amount of MTrk blocks used */
	private static final byte[] MIDI_HEADER_MTRK_CHUNK_AMOUNT = {0x00, 0x01};

	/* Value for first byte in PPQN block in midi file header */
	private static final byte MIDI_HEADER_PPQN_FIRST_BYTE = 0x00;

	// MIDI track constants

	/* Header block for MTrk */
	private static final byte[] MIDI_HEADER_MTRK = {0x4D, 0x54, 0x72, 0x6B};

	/* Tempo change command. Includes delta time ( 0x00 )
	   and command (0xFF5103) */
	private static final byte[] TRACK_HEADER_TEMPO_CHANGE = {0x00, (byte) 0xFF, 0x51, 0x03};

	/* Track end meta event */
	private static final byte[] TRACK_TRAILER = {0x00, (byte) 0xFF, 0x2F, 0x00};

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
	private final byte channel;

	/* Tempo in MIDI terms */
	private int tempo;

	/* Resolution in MIDI terms */
	private int resolution;

	/* Instrument used to represent tone */
	private final byte instrument;

	/* Counter for written midi events */
	private int midiEventsByteCount;

	/* MIDI sequence written using writeEvent( ) */
	private final ByteArrayOutputStream midiTrackEvents;

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
		int size = FILE_HEADER_LENGTH + MTRK_HEADER_LENGTH + TRACK_HEADER_LENGTH +
				midiTrackEvents.length + TRACK_TRAILER_LENGTH;
		ByteArrayOutputStream concateStream = new ByteArrayOutputStream(size);

		writeHeader(concateStream, midiTrackEvents.length);
		concateStream.write(midiTrackEvents);
		writeTrailer(concateStream);

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
		int size = FILE_HEADER_LENGTH + MTRK_HEADER_LENGTH + TRACK_HEADER_LENGTH +
				midiTrackEvents.length + TRACK_TRAILER_LENGTH;
		ByteArrayOutputStream concateStream = new ByteArrayOutputStream(size);

		writeHeader(concateStream, midiTrackEvents.length);
		concateStream.write(midiTrackEvents);
		writeTrailer(concateStream);

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
		midiEventsByteCount += writeVarLen(midiTrackEvents, length);

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
	 * Write time interval value as MIDI variable length data to byte array.
	 *
	 * @param out   output stream
	 * @param value time before the event in question happens, relative to
	 *              current time. Must be between 0 and 0x0FFFFFFF
	 */
	private int writeVarLen(ByteArrayOutputStream out, int value) {
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
			out.write(buffer);
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
	 * Writes midi header
	 *
	 * @param aOut              output stream
	 * @param aMidiEventsLength lenght of midi event content in bytes
	 */
	private void writeHeader(ByteArrayOutputStream aOut, int aMidiEventsLength) throws IOException {
		// MIDI FILE HEADER

		// write 'MThd' block id
		aOut.write(MIDI_HEADER_MTHD);

		// write MThd block length
		aOut.write(MIDI_HEADER_MTHD_LENGTH);

		// write midi format; format is 0
		aOut.write(MIDI_HEADER_MIDI_FORMAT);

		// write MTrk chunk amount; only one track
		aOut.write(MIDI_HEADER_MTRK_CHUNK_AMOUNT);

		// write PPQN resolution (pulses per quarternote)
		aOut.write(MIDI_HEADER_PPQN_FIRST_BYTE);
		aOut.write(resolution);

		// MTrk HEADER

		// write 'MTrk' for the only track
		aOut.write(MIDI_HEADER_MTRK);

		// calculate real track length
		int trackLength = TRACK_HEADER_LENGTH + aMidiEventsLength + TRACK_TRAILER_LENGTH;

		// write track length in bytes.
		// Literal numeric values (24,16,8) indicate shift offset in bits
		// 0xFF is used to mask out everything but the lowest byte.
		aOut.write((trackLength >> 24) & 0xFF);
		aOut.write((trackLength >> 16) & 0xFF);
		aOut.write((trackLength >> 8) & 0xFF);
		aOut.write(trackLength & 0xFF);

		// TRACK HEADER

		// write tempo change at beginning
		aOut.write(TRACK_HEADER_TEMPO_CHANGE);

		// calculate tempo in microseconds per quarter note
		int mpqn = MINUTE_AS_MICROSECONDS / tempo;

		// write tempo value
		// Literal numeric values (16,8) indicate shift offset in bits
		// 0xFF is used to mask out everything but the lowest byte.
		aOut.write((mpqn >> 16) & 0xFF);
		aOut.write((mpqn >> 8) & 0xFF);
		aOut.write(mpqn & 0xFF);

		// change program at beginning (at delta time 0)
		writeVarLen(aOut, 0);
		aOut.write((byte) (MidiToneConstants.MIDI_PROGRAM_CHANGE | channel));
		aOut.write(instrument);   // instrument number
	}

	/**
	 * Write midi trailer
	 *
	 * @param aOut output stream
	 */
	private void writeTrailer(ByteArrayOutputStream aOut) throws IOException {
		aOut.write(TRACK_TRAILER);
	}

	/**
	 * Return duration accumulated so far.
	 *
	 * @return long duration in microseconds
	 */
	public long getCumulativeDuration() {
		// duration * seconds in minute * microseconds in second /
		// (resolution * tempo)
		return (long) duration * 60 * 1000000 / ((long) resolution * tempo);
	}
}
