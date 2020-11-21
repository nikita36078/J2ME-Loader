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
 * Description:  This class is holder/handler for other event types
 *
 */

package javax.microedition.media.tone;

import java.util.Enumeration;
import java.util.Vector;

/**
 * This class is holder/handler for other event types
 */
public class EventList extends Event {
	private static final int EVENT_PROCESSOR_COUNT = 6;

	private final Vector<Event> events;

	EventList(byte[] sequence, MidiSequence midiSequence) {
		super(sequence, midiSequence);

		events = new Vector<>(EVENT_PROCESSOR_COUNT);

		events.addElement(new ToneEvent(sequence, midiSequence));
		events.addElement(new BlockStartEvent(sequence, midiSequence));
		events.addElement(new BlockEndEvent(sequence, midiSequence));
		events.addElement(new PlayBlockEvent(sequence, midiSequence));
		events.addElement(new RepeatEvent(sequence, midiSequence));
		events.addElement(new VolumeEvent(sequence, midiSequence));
	}

	public int advance(int position) throws MidiSequenceException {
		// first check that we have at least two bytes left in iSequence
		// in position.
		if (sequence.length - position < EVENT_SIZE) {
			// return with 0 if end of sequence is reached
			return 0;
		}

		Event event = null;
		int retVal = 0;

		for (Enumeration<Event> e = events.elements(); e.hasMoreElements(); ) {
			event = e.nextElement();
			retVal = event.advance(position);
			if (retVal != 0) {
				return retVal;
			}
		}
		// if none of event processors accepts data at current position,
		// parameter data is illegal.
		throw new IllegalArgumentException("Illegal event found, sequence is corrupted");
	}

	/**
	 * Inherited from Event.
	 * Special definition for validate. EventList is the main
	 * class performing the actual validation and is thus
	 * excempt from the usual validation process.
	 *
	 * @param position position to validate
	 */
	public int validate(int position) throws IllegalArgumentException {
		Event event = null;
		int retVal = 0;
		for (Enumeration<Event> e = events.elements(); e.hasMoreElements(); ) {
			event = e.nextElement();
			retVal = event.validate(position);
			if (retVal != 0) {
				return retVal;
			}
		}
		// if none of event processors accepts data at current position,
		// parameter data is illegal.
		throw new IllegalArgumentException("Illegal event found, sequence is corrupted");
	}

	/**
	 * Child class defined functionality for validate
	 *
	 * @param position position in tone sequence array where to validate
	 * @return new position offset related to position in tone sequence
	 * array. Must be positive.
	 */
	protected int doValidate(int position) throws IllegalArgumentException {
		throw new Error("Illegal validation call");
	}

	/**
	 * Child class defined functionality for checkEventAtNextPosition
	 *
	 * @param position position in tone sequence array where to check
	 */
	protected void checkEventAtNextPosition(int position) throws IllegalArgumentException {
		throw new Error("Illegal validation call");
	}
}
