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
 * Description:  Base class for tone event processors
 *
 */

package javax.microedition.media.tone;

import android.util.Log;

import java.util.Stack;

/**
 * Base class for tone event processors
 */
public abstract class Event {
	private static final String TAG = Event.class.getName();

	/* Size of single tone event in bytes */
	public static final byte EVENT_SIZE = 2;

	/* Hold original tone sequence bytes */
	protected byte[] sequence;

	/* Holds the new tone sequence converted to MIDI */
	protected MidiSequence midiSequence;

	/* Stack for tone event processors. Used by the method advance. */
	protected static Stack<Integer> returnPositionStack;

	/* Current block number holder for validating blocks. If not in any
	   block, the value is -1. Manipulated by accessor methods. */
	private static Stack<Integer> currentBlockNumStack;

	// Static initialization
	static {
		returnPositionStack = new Stack<>();
		currentBlockNumStack = new Stack<>();
	}

	protected Event(byte[] sequence, MidiSequence midiSequence) {
		this.sequence = sequence;
		this.midiSequence = midiSequence;
	}

	/**
	 * Reset events for reuse
	 */
	public void reset() {
		if (!returnPositionStack.empty()) {
			returnPositionStack = new Stack<>();
		}
		if (!currentBlockNumStack.isEmpty()) {
			currentBlockNumStack = new Stack<>();
		}
	}

	/**
	 * Process events step by step. Does not validate and does not go
	 * through whole sequence, but only those positions that are
	 * needed for playing.
	 *
	 * @param position position where to process
	 * @return int offset of position after processing, relative to position.
	 * If zero, no processing has happened.
	 */
	public abstract int advance(int position) throws MidiSequenceException, IllegalArgumentException;

	/**
	 * Child class defined functionality for validate
	 *
	 * @param position position in tone sequence array where to validate
	 * @return new position offset related to position in tone sequence
	 * array. Must be positive.
	 */
	protected abstract int doValidate(int position) throws IllegalArgumentException;

	/**
	 * Validate sequence step by step. Does not process events, but can do
	 * initialization along validation. Validation includes: Check event
	 * type, parameters and that next event type after this one is allowed
	 * to be there.
	 *
	 * @param position position where to validate
	 * @return new position offset related to position in tone sequence
	 * array. Must be positive.
	 */
	public int validate(int position) throws IllegalArgumentException {
		int nextPos = 0;
		try {
			nextPos = doValidate(position);
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			Log.w(TAG, "MMA: Event: validate: AIOOBE");
			// just return
			return nextPos;
		}

		if (nextPos < 0) {
			// doValidate must never return negative value
			throw new Error();
		}

		if (nextPos != 0) {
			// check that event type in next position is allowed
			checkEventAtNextPosition(position + nextPos);
		}
		return nextPos;
	}

	/**
	 * Check whether event or end of sequence at next position
	 * is legal after this type of event. Throws
	 * IllegalArgumentException if not accepted.
	 *
	 * @param position position of next event
	 */

	protected abstract void checkEventAtNextPosition(int position) throws IllegalArgumentException;

	/**
	 * Called when entering a block.
	 *
	 * @param blockNum number of block to enter
	 */
	protected void enterBlock(int blockNum) {
		currentBlockNumStack.push(blockNum);
	}

	/**
	 * Called when leaving a block.
	 *
	 * @param blockNum number of block to leave
	 * @throws IllegalArgumentException if blockNum does not
	 *                                  correspond to last block entered or if no block has been
	 *                                  entered.
	 */
	protected void leaveBlock(int blockNum) throws IllegalArgumentException {
		if (currentBlockNumStack.isEmpty()) {
			Log.e(TAG, "MMA: Event: leaveBlock: Not inside block, IAE");
			throw new IllegalArgumentException("Illegal Sequence, invalid block number found");
		}

		if (blockNum != currentBlockNumStack.pop()) {
			Log.e(TAG, "MMA: Event: leaveBlock: Incorrect block number, IAE");
			throw new IllegalArgumentException("Illegal Sequence, invalid block number found");
		}
	}
}
