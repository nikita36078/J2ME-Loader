package com.sonivox.mmapi;

import javax.microedition.media.control.TempoControl;

/**
 * Implementation of TempoControl for EAS.
 */
public class ControlTempo extends ControlRate implements TempoControl {

	/**
	 * the cached tempo
	 */
	private int tempo = -1;

	/**
	 * Create a new instance of this EAS tempo control.
	 * 
	 * @param player the owning player
	 */
	ControlTempo(PlayerEAS player) {
		super(player);
	}

	/* (non-Javadoc)
	 * @see javax.microedition.media.control.TempoControl#getTempo()
	 */
	public int getTempo() {
		if (tempo < 0) {
			tempo = EAS.getPlaybackTempo(player.handle);
		}	
		// if error, just return default tempo
		return (tempo > 0)?tempo:120000;
	}

	/* (non-Javadoc)
	 * @see javax.microedition.media.control.TempoControl#setTempo(int)
	 */
	public int setTempo(int millitempo) {
		tempo = EAS.setPlaybackTempo(player.handle, millitempo);
		return getTempo();
	}

}
