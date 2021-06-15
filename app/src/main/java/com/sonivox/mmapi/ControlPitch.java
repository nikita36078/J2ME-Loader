package com.sonivox.mmapi;

import javax.microedition.media.control.PitchControl;

/**
 * Implementation of PitchControl for EAS.
 */
class ControlPitch extends ControlBase implements PitchControl {

	/**
	 * the cached pitch in milli-semitones
	 */
	private int pitch; // = 0;

	/**
	 * Create a new instance of this EAS pitch control.
	 * 
	 * @param player the owning player
	 */
	ControlPitch(PlayerEAS player) {
		super(player);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.PitchControl#setPitch(int)
	 */
	public int setPitch(int millisemitones) {
		if (millisemitones < getMinPitch()) {
			millisemitones = getMinPitch();
		} else if (millisemitones > getMaxPitch()) {
			millisemitones = getMaxPitch();
		}
		if (millisemitones != pitch) {
			if (player.handle != 0) {
				pitch = EAS.setTransposition(player.handle, millisemitones);
			} else {
				pitch = millisemitones;
			}
		}
		return pitch;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.PitchControl#getPitch()
	 */
	public int getPitch() {
		return pitch;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.PitchControl#getMaxPitch()
	 */
	public int getMaxPitch() {
		return Config.PITCH_MAX;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.PitchControl#getMinPitch()
	 */
	public int getMinPitch() {
		return Config.PITCH_MIN;
	}

}
