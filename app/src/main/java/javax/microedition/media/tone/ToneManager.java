/*
 * Copyright (c) 2006 Nokia Corporation and/or its subsidiary(-ies).
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
 * Description:  Manager.playTone implementation
 *
 */

package javax.microedition.media.tone;

import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.ToneControl;

/**
 * Manager.playTone implementation
 */
public class ToneManager {
	private static final String TONE_SEQUENCE_CONTENT_TYPE = "audio/x-tone-seq";
	private static final int TONE_SEQUENCE_VERSION = 1;
	private static final int TONE_SEQUENCE_RESOLUTION = 64;
	private static final int TONE_SEQUENCE_TEMPO = 30;
	private static final int DURATION_DIVIDE = 240000;
	private static final String CANNOT_PLAY_TONE = "Cannot play tone";

	public static void play(int note, int duration, int volume) throws MediaException {
		Player p = createPlayer(note, duration, volume);
		try {
			p.start();
		} catch (MediaException me) {
			throw me;
		}
		new Thread(() -> {
			try {
				Thread.sleep(duration * 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			p.deallocate();
		}).start();
	}

	public static Player createPlayer(int note, int duration, int volume) throws MediaException {
		if (volume < MidiToneConstants.TONE_MIN_VOLUME) {
			volume = MidiToneConstants.TONE_MIN_VOLUME;
		} else if (volume > MidiToneConstants.TONE_MAX_VOLUME) {
			volume = MidiToneConstants.TONE_MAX_VOLUME;
		}

		if (note > MidiToneConstants.TONE_MAX_NOTE || note < MidiToneConstants.TONE_MIN_NOTE) {
			throw new IllegalArgumentException("Note is out of range, " +
					"valid range is 0 <= Note <= 127");
		}

		if (duration <= 0) {
			throw new IllegalArgumentException("Duration must be positive");
		}

		int curDuration = duration * TONE_SEQUENCE_RESOLUTION *
				TONE_SEQUENCE_TEMPO / DURATION_DIVIDE;

		if (curDuration < MidiToneConstants.TONE_SEQUENCE_NOTE_MIN_DURATION) {
			curDuration = MidiToneConstants.TONE_SEQUENCE_NOTE_MIN_DURATION;
		} else if (curDuration > MidiToneConstants.TONE_SEQUENCE_NOTE_MAX_DURATION) {
			curDuration = MidiToneConstants.TONE_SEQUENCE_NOTE_MAX_DURATION;
		}

		byte[] sequence = {
				ToneControl.VERSION, TONE_SEQUENCE_VERSION,
				ToneControl.TEMPO, TONE_SEQUENCE_TEMPO,
				ToneControl.RESOLUTION, TONE_SEQUENCE_RESOLUTION,
				ToneControl.SET_VOLUME, (byte) volume,
				(byte) note, (byte) curDuration
		};

		Player p = null;
		try {
			p = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
		} catch (IOException ioe) {
			throw new MediaException(CANNOT_PLAY_TONE + " " + ioe.getMessage());
		}
		ToneControl toneControl = (ToneControl) p.getControl("ToneControl");
		toneControl.setSequence(sequence);
		return p;
	}
}
