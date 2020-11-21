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
 * Description:  This class represents tone sequence for tone to midi conversion
 *
 */

package javax.microedition.media.tone;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.microedition.media.control.ToneControl;

/**
 * This class represents tone sequence for tone to midi conversion
 */
public class ToneSequence {
	private static final String TAG = ToneSequence.class.getName();

	/* Hold original tone sequence bytes */
	private byte[] toneSequence;

	/* Holds the new tone sequence converted to MIDI */
	private MidiSequence midiSequence;

	/* Event list used to hold tone event processors */
	private EventList eventList;

	public ToneSequence(byte[] sequence) {
		toneSequence = sequence;
		midiSequence = new MidiSequence(MidiToneConstants.MIDI_TONE_CHANNEL,
				MidiToneConstants.MIDI_TONE_INSTRUMENT);
		eventList = new EventList(toneSequence, midiSequence);
	}

	public void process() {
		// Reset static base class variables of events before processing.
		eventList.reset();

		// Check input; tone sequence must be even length
		// ie. multiple of event size
		if ((toneSequence.length % Event.EVENT_SIZE) != 0) {
			throw new IllegalArgumentException(
					"Illegal sequence, tone sequence must be multiple of single tone event size (2)");
		}

		// Validate header bytes

		// Next check that have correct VERSION and possibly
		// VERSION and RESOLUTION.
		int checkPos = 0;

		// First two bytes must belong to VERSION
		if (toneSequence[checkPos] != ToneControl.VERSION ||
				toneSequence[checkPos + 1] != MidiToneConstants.TONE_SEQUENCE_SUPPORTED_VERSION) {
			throw new IllegalArgumentException(
					"Illegal sequence, first two bytes must belong to version");
		}

		// Next two may be TEMPO or RESOLUTION
		checkPos += Event.EVENT_SIZE;

		if (toneSequence[checkPos] == ToneControl.TEMPO) {
			midiSequence.setTempo(toneSequence[checkPos + 1]);
			checkPos += Event.EVENT_SIZE;
		}

		// Continue checking RESOLUTION
		if (toneSequence[checkPos] == ToneControl.RESOLUTION) {
			midiSequence.setResolution(toneSequence[checkPos + 1]);
			checkPos += Event.EVENT_SIZE;
		}

		// Validate rest of the sequence

		int count = 0; // Offset to new position in tone sequence. >= 0
		while (checkPos < toneSequence.length) {
			count = eventList.validate(checkPos);
			checkPos += count;
			if (count == 0) {
				// if end of tone sequence is reached, zero is
				// OK. Otherwise this indicates error
				if (checkPos != toneSequence.length) {
					throw new IllegalArgumentException("Validation failed, sequence corrupted");
				}
				break;
			}
		}

		// Find start of sequence
		int position = 0; // current position on tone sequence

		for (int i = toneSequence.length - Event.EVENT_SIZE; i >= 0; i -= Event.EVENT_SIZE) {
			// There cannot be any lower value command bytes in tone sequence
			// than REPEAT
			if (toneSequence[i] < ToneControl.REPEAT) {
				throw new IllegalArgumentException(
						"Illegal sequence, lower value command than ToneControl.REPEAT found");
			}

			if (toneSequence[i] < ToneControl.SILENCE &&
					toneSequence[i] != ToneControl.PLAY_BLOCK &&
					toneSequence[i] != ToneControl.SET_VOLUME &&
					toneSequence[i] != ToneControl.REPEAT) {
				position = i + Event.EVENT_SIZE;
				// stop the for loop
				break;
			}
		}

		// No start position found
		if (position < Event.EVENT_SIZE) {
			throw new IllegalArgumentException("Illegal sequence, no start position found");
		}

		count = 0; // offset to new position in tone sequence. +/-
		try {
			while (position > 0 && position < toneSequence.length) {
				count = eventList.advance(position);
				position += count;
				if (count == 0) {
					// if end of tone sequence is reached, zero is
					// OK. Otherwise this indicates error
					if (position != toneSequence.length) {
						throw new IllegalArgumentException("Validation failed, sequence corrupted");
					}
					break;
				}
			}
		} catch (MidiSequenceException mse) {
			// This exception indicates that we have reached the maximum
			// midi sequence length and thus must stop processing. Currently
			// processed midi sequence is however available by getStream.
			// So no action is needed here.
			Log.w(TAG, "MMA: ToneSequence: MIDI maximum lenght reached.");
		}
	}

	public ByteArrayInputStream getStream() throws IOException {
		return midiSequence.getStream();
	}

	public byte[] getByteArray() throws IOException {
		return midiSequence.getByteArray();
	}

	/**
	 * Get duration of tone sequence
	 */
	public long getDuration() {
		return midiSequence.getCumulativeDuration();
	}
}
