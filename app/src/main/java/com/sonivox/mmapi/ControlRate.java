package com.sonivox.mmapi;

import javax.microedition.media.control.RateControl;

/**
 * Implementation of RateControl for EAS.
 */
class ControlRate extends ControlBase implements RateControl {

	/**
	 * the cached rate
	 */
	private int rate = 100000;

	/**
	 * Create a new instance of this EAS rate control.
	 * 
	 * @param player the owning player
	 */
	ControlRate(PlayerEAS player) {
		super(player);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RateControl#setRate(int)
	 */
	public int setRate(int millirate) {
		if (millirate < getMinRate()) {
			millirate = getMinRate();
		} else if (millirate > getMaxRate()) {
			millirate = getMaxRate();
		}
		if (millirate != rate) {
			if (player.handle != 0) {
				rate = EAS.setPlaybackRate(player.handle, millirate);
			} else {
				rate = millirate;
			}
		}
		return rate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RateControl#getRate()
	 */
	public int getRate() {
		return rate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RateControl#getMaxRate()
	 */
	public int getMaxRate() {
		return Config.RATE_MAX;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.RateControl#getMinRate()
	 */
	public int getMinRate() {
		return Config.RATE_MIN;
	}

}
