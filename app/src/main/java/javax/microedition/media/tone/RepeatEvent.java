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
 * Description:  Event processor class for tone sequence REPEAT events
 *
 */

package javax.microedition.media.tone;

import javax.microedition.media.control.ToneControl;

/**
 * Event processor class for tone sequence REPEAT events
 */
public class RepeatEvent extends Event {
	/* Maximum and minimum tone repeat multiplier values */
	public static final int REPEAT_MIN_MULTIPLIER = 2;
	public static final int REPEAT_MAX_MULTIPLIER = 127;

	/**
	 * RepeatEvent constructor
	 *
	 * @param sequence     tone sequence byte array (input)
	 * @param midiSequence midi sequence object where to output midi events.
	 */

	RepeatEvent(byte[] sequence, MidiSequence midiSequence) {
		super(sequence, midiSequence);
	}

	/**
	 * Inherited from Event
	 */
	public int advance(int position) throws MidiSequenceException {
		// it is already checked that there is at least two bytes left
		byte type = sequence[position];
		byte data = sequence[position + 1];
		int retVal = doValidate(position);
		if (retVal == 0) {
			return 0;
		}
		for (int i = 0; i < data; i++) {
			retVal = ToneEvent.staticAdvance(position + EVENT_SIZE,
					sequence,
					midiSequence);
			// This would go unnoticed if not checked here.
			if (retVal == 0) {
				throw new IllegalArgumentException(
						"Illegal sequence, processing events for playing failed");
			}
			retVal = EVENT_SIZE + EVENT_SIZE;
		}
		return retVal;
	}

	/**
	 * Child class defined functionality for validate
	 *
	 * @param position position in tone sequence array where to validate
	 * @return new position offset related to position in tone sequence
	 * array. Must be positive.
	 */
	protected int doValidate(int position) throws IllegalArgumentException {
		byte type = sequence[position];
		byte data = sequence[position + 1];
		int retVal = 0;
		if (type == ToneControl.REPEAT) {
			if (data < REPEAT_MIN_MULTIPLIER || data > REPEAT_MAX_MULTIPLIER) {
				throw new IllegalArgumentException(
						"Repeat multiplier out of range, valid range is 2 <= multiplier <= 127");
			}

			// Check that there is two more bytes available
			if (sequence.length - (position + EVENT_SIZE) < EVENT_SIZE) {
				throw new IllegalArgumentException(
						"Illegal ToneControl.REPEAT event, " +
								"there should be two more bytes available after REPEAT event");
			}
			retVal = EVENT_SIZE + EVENT_SIZE;
		}
		return retVal;
	}

	/**
	 * Child class defined functionality for checkEventAtNextPosition
	 *
	 * @param position position in tone sequence array where to check
	 */
	protected void checkEventAtNextPosition(int position) throws IllegalArgumentException {
		// After this event there can be:
		// Tone, BlockEnd, PlayBlock, Volume, Repeat or
		// end of sequence

		int type = 0;
		try {
			type = sequence[position];
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			return; // end of sequence is ok for this event
		}

		if (type >= ToneControl.SILENCE ||
				type == ToneControl.BLOCK_END ||
				type == ToneControl.PLAY_BLOCK ||
				type == ToneControl.SET_VOLUME ||
				type == ToneControl.REPEAT) {
			return;
		}

		throw new IllegalArgumentException("Illegal event found; sequence is corrupted");
	}
}
