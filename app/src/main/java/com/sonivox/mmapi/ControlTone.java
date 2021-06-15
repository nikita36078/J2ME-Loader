package com.sonivox.mmapi;

import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.ToneControl;

/**
 * Implementation of ToneControl for EAS.
 */
class ControlTone extends ControlBase implements ToneControl {

	/**
	 * Create a new instance of this EAS pitch control.
	 * 
	 * @param player the owning player
	 */
	ControlTone(PlayerEAS player) {
		super(player);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.ToneControl#setSequence(byte[])
	 */
	public void setSequence(byte[] sequence) {
		if (player.getState() != Player.REALIZED) {
			throw new IllegalStateException("illegal state");
		}
		if (sequence == null) {
			throw new IllegalArgumentException("sequence is null");
		}
		int len = sequence.length;
		// only support tone sequence version 1
		if (len < 4 || sequence[0] != VERSION || sequence[1] != 1) {
			throw new IllegalArgumentException("sequence is invalid");
		}
		try {
			int ret = EAS.write(player.handle, sequence, 0, len, len, false);
			if (ret < len) {
				throw new IllegalArgumentException("error queuing the sequence");
			}
			player.calcDuration(true);
		} catch (MediaException me) {
			throw new IllegalArgumentException(me.getMessage());
		}
	}

}
