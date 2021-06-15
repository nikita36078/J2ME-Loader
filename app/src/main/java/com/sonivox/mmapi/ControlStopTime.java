package com.sonivox.mmapi;

import javax.microedition.media.*;
import javax.microedition.media.control.StopTimeControl;

/**
 * StopTimeControl implementation for EAS.
 */
class ControlStopTime extends ControlBase implements StopTimeControl {

	private long stopTime = RESET;

	/**
	 * Create a new instance of this EAS stop time control.
	 * 
	 * @param player the owning player
	 */
	ControlStopTime(PlayerEAS player) {
		super(player);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.StopTimeControl#setStopTime(long)
	 */
	public void setStopTime(long stopTime) {
		if (player.getState() == Player.STARTED && this.stopTime != RESET) {
			throw new IllegalStateException(
					"cannot set new stop time during playback");
		}
		this.stopTime = stopTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.StopTimeControl#getStopTime()
	 */
	public long getStopTime() {
		return stopTime;
	}

}
