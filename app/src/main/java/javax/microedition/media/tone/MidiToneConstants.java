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
 * Description:  Midi and tone related constants
 *
 */

package javax.microedition.media.tone;

/**
 * MIDI and tone related common constants
 */
public class MidiToneConstants {

	// Fully qualified name for ToneControl
	public static final String TONE_CONTROL_FULL_NAME =
			"javax.microedition.media.control.ToneControl";

	// MIDI constants

	// Note on message ID
	public static final byte MIDI_NOTE_ON = (byte) 0x90;

	// Note off message ID
	public static final byte MIDI_NOTE_OFF = (byte) 0x80;

	// Control change message ID
	public static final byte MIDI_CONTROL_CHANGE = (byte) 0xB0;

	// Program change message ID
	public static final byte MIDI_PROGRAM_CHANGE = (byte) 0xC0;

	// MIDI Main volume control ID for control change message
	public static final byte MIDI_CONTROL_MAIN_VOLUME = 0x07;

	// MIDI velocity for tone notes
	public static final byte MIDI_MAX_VELOCITY = 127;
	public static final byte MIDI_MIN_VELOCITY = 0;

	// Maximum and minimum MIDI volume values
	public static final byte MIDI_MAX_VOLUME = 127;
	public static final byte MIDI_MIN_VOLUME = 0;

	// Maximum and minimum MIDI volume values
	public static final byte MIDI_MAX_TONE = 127;
	public static final byte MIDI_MIN_TONE = 0;

	// MIDI instrument used to play tone events
	public static final byte MIDI_TONE_INSTRUMENT = 39;

	// MIDI bank for instrument used to play tone events
	public static final byte MIDI_TONE_BANK = -1;

	// MIDI channel used to play tone events
	public static final byte MIDI_TONE_CHANNEL = 0;

	// Tone MidiToneConstants

	// Maximum and minimum tone volume values
	public static final byte TONE_MAX_VOLUME = 100;
	public static final byte TONE_MIN_VOLUME = 0;

	// Maximum and minimum tone note values
	public static final byte TONE_MAX_NOTE = 127;
	public static final byte TONE_MIN_NOTE = 0;

	// Maximum and minimum block number values
	public static final byte TONE_MAX_BLOCK = 127;
	public static final byte TONE_MIN_BLOCK = 0;

	/* Minimum and maximum values for note duration */
	public static final int TONE_SEQUENCE_NOTE_MAX_DURATION = 127;
	public static final int TONE_SEQUENCE_NOTE_MIN_DURATION = 1;

	/* Minimun tone sequence length */
	public static final int TONE_SEQUENCE_MIN_LENGTH = 4;

	/* Supported tone version number */
	public static final int TONE_SEQUENCE_SUPPORTED_VERSION = 1;

	/* Minimum and maximum tone tempo values */
	public static final int TONE_TEMPO_MIN = 5;
	public static final int TONE_TEMPO_MAX = 127;

	/* Minimum and maximum tone resolution values */
	public static final int TONE_RESOLUTION_MIN = 1;
	public static final int TONE_RESOLUTION_MAX = 127;
}
