package com.sonivox.mmapi;

import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

/**
 * Implementation of VolumeControl for EAS.
 */
class ControlVolume extends ControlBase implements VolumeControl {

	/**
	 * true if muted
	 */
	private boolean muted; // = false

	/**
	 * Cache of the current volume level.
	 */
	private int level = -1;

	/**
	 * Create this volume control for the player.
	 * 
	 * @param player thw owning player
	 */
	ControlVolume(PlayerEAS player) {
		super(player);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.VolumeControl#setMute(boolean)
	 */
	public void setMute(boolean mute) {
		if (mute && !isMuted()) {
			level = getLevel();
			EAS.setLevel(player.handle, 0);
			muted = true;
			sendMessage();
		} else if (!mute && isMuted()) {
			EAS.setLevel(player.handle, level);
			muted = false;
			sendMessage();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.VolumeControl#isMuted()
	 */
	public boolean isMuted() {
		return muted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.VolumeControl#setLevel(int)
	 */
	public int setLevel(int level) {
		if (level < 0) level = 0;
		if (level > 100) level = 100;
		if (this.level != level) {
			if (player.handle == 0 || isMuted()
					|| EAS.setLevel(player.handle, level)) {
				this.level = level;
				sendMessage();
			}
		}
		return this.level;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.VolumeControl#getLevel()
	 */
	public int getLevel() {
		// may return -1 if not yet realized
		if (!muted && player.handle != 0) {
			level = EAS.getLevel(player.handle);
		}
		return level;
	}

	/**
	 * Send a VOLUME_CHANGED event.
	 */
	private void sendMessage() {
		player.dispatchMessage(PlayerListener.VOLUME_CHANGED, this);
	}

}
