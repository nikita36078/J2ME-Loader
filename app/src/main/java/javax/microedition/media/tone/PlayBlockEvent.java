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
 * Description:  Event processor class for tone sequence PLAY_BLOCK events
 *
 */

package javax.microedition.media.tone;

import javax.microedition.media.control.ToneControl;

/**
 * Processor for play block events
 */
public class PlayBlockEvent extends Event {
	/* Maximum and minimum values for block number */
	public static final int PLAY_BLOCK_MAX_VALUE = 127;
	public static final int PLAY_BLOCK_MIN_VALUE = 0;

	PlayBlockEvent(byte[] sequence, MidiSequence midiSequence) {
		super(sequence, midiSequence);
	}

	/**
	 * Inherited from Event
	 */
	public int advance(int position) {
		int retVal = doValidate(position);
		if (retVal != 0) {
			// Push the position to stack that is going to be
			// played after this play block has completed
			returnPositionStack.push(position + EVENT_SIZE);

			int data = sequence[position + 1];
			retVal = findBlock(data) - position;
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
		// it is already checked that there is at least two bytes left
		int type = sequence[position];
		int data = sequence[position + 1];
		int retVal = 0;

		if (type == ToneControl.PLAY_BLOCK) {
			if (data < PLAY_BLOCK_MIN_VALUE) {
				throw new IllegalArgumentException("Block number out of range");
			}
			findBlock(data);   // for check only
			retVal = EVENT_SIZE;
		}
		return retVal;
	}

	/**
	 * Find block
	 *
	 * @param blockNumber number
	 * @return position of corresponding BLOCK_START event
	 * @throws IllegalArgumentException if block is not found
	 */
	private int findBlock(int blockNumber) {
		for (int i = 0; i < sequence.length; i += EVENT_SIZE) {
			if (sequence[i] == ToneControl.BLOCK_START && sequence[i + 1] == blockNumber) {
				return i;
			}
		}
		// if block is not found, input is illegal
		throw new IllegalArgumentException("No block found, sequence is corrupted");
	}

	/**
	 * Child class defined functionality for checkEventAtNextPosition
	 *
	 * @param position position in tone sequence array where to check
	 */
	protected void checkEventAtNextPosition(int position) throws IllegalArgumentException {
		// After this event there can be:
		// Tone, BlockEnd PlayBlock, Volume, Repeat or end of sequence

		int type;
		try {
			type = sequence[position];
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			return; // end of sequence is OK
		}

		if (type >= ToneControl.SILENCE ||
				type == ToneControl.BLOCK_END ||
				type == ToneControl.PLAY_BLOCK ||
				type == ToneControl.SET_VOLUME ||
				type == ToneControl.REPEAT) {
			return;
		}
		throw new IllegalArgumentException("Illegal event found, sequence is corrupted");
	}
}
