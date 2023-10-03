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

import android.util.Log;

import java.util.Vector;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.ToneControl;

/** Manager.playTone implementation */
public class ToneManager implements PlayerListener {
	private static final String TAG = ToneManager.class.getSimpleName();

	private static final int TONE_SEQUENCE_VERSION = 1;
	private static final int TONE_SEQUENCE_RESOLUTION = 64;
	private static final int TONE_SEQUENCE_TEMPO = 30;
	private static final int DURATION_DIVIDE = 240000;
	private static final String CANNOT_PLAY_TONE = "Cannot play tone";

	/** @noinspection MismatchedQueryAndUpdateOfCollection*/
	// Holds tone players
	private final Vector<Player> players = new Vector<>();

	private ToneManager() {}

	public static ToneManager getInstance() {
		return InstanceHolder.instance;
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

		int tsDuration = duration * TONE_SEQUENCE_RESOLUTION * TONE_SEQUENCE_TEMPO / DURATION_DIVIDE;

		if (tsDuration < MidiToneConstants.TONE_SEQUENCE_NOTE_MIN_DURATION) {
			tsDuration = MidiToneConstants.TONE_SEQUENCE_NOTE_MIN_DURATION;
		} else if (tsDuration > MidiToneConstants.TONE_SEQUENCE_NOTE_MAX_DURATION) {
			tsDuration = MidiToneConstants.TONE_SEQUENCE_NOTE_MAX_DURATION;
		}

		byte[] sequence = {
				ToneControl.VERSION, TONE_SEQUENCE_VERSION,
				ToneControl.TEMPO, TONE_SEQUENCE_TEMPO,
				ToneControl.RESOLUTION, TONE_SEQUENCE_RESOLUTION,
				ToneControl.SET_VOLUME, (byte) volume,
				(byte) note, (byte) tsDuration
		};

		try {
			Player player = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
			player.realize();
			ToneControl control = (ToneControl) player.getControl(MidiToneConstants.TONE_CONTROL_FULL_NAME);
			control.setSequence(sequence);
			return player;
		} catch (Exception e) {
			Log.e(TAG, "createPlayer: " + CANNOT_PLAY_TONE, e);
			throw new MediaException(CANNOT_PLAY_TONE);
		}
	}

	/**
	 * Play tone.
	 *
	 * @see Manager#playTone(int, int, int)
	 */
	synchronized public void playTone(int note, int duration, int volume) throws MediaException {
		Player p = createPlayer(note, duration, volume);

		p.addPlayerListener(this);
		players.addElement(p);
		try {
			p.start();
		} catch (MediaException me) {
			players.removeElement(p);
			throw me;
		}
	}

	public void playerUpdate(Player player, String event, Object eventData) {
		if (END_OF_MEDIA.equals(event) || ERROR.equals(event)) {
			player.close();
			players.removeElement(player);
		}
	}

	private static final class InstanceHolder {
		static final ToneManager instance = new ToneManager();
	}
}
