package com.sonivox.mmapi;

/**
 * Base class for all controls for the EAS implementation.
 */
class ControlBase {

	/**
	 * Reference to the owning player.
	 */
	protected PlayerEAS player; // = null
	
	/**
	 * Create this control for the player.
	 * @param player the owning player
	 */
	protected ControlBase(PlayerEAS player) {
		this.player = player;
	}
	
}
