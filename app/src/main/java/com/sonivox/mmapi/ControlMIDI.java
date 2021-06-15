package com.sonivox.mmapi;

import javax.microedition.media.*;
import javax.microedition.media.control.MIDIControl;

/**
 * Implementation of MIDIControl for EAS.
 */
class ControlMIDI extends ControlBase implements MIDIControl {

	/**
	 * The handle to the interactive EAS MIDI device (stream), for MIDIControl's
	 * retrieved from MIDI files.
	 */
	private int handle; // = 0;

	/**
	 * Create a new instance of this EAS pitch control.
	 * 
	 * @param player the owning player
	 * @throws MediaException for MIDI file players, if EAS cannot open the MIDI
	 *             stream interface.
	 */
	ControlMIDI(PlayerEAS player) throws MediaException {
		super(player);
		if (player.getPlayerType() == PlayerEAS.TYPE_MIDI_PLAYER) {
			// need to open the native hook
			this.handle = EAS.openInteractiveMIDI(player.handle);
		}
		;
	}
	
	/**
	 * @return true if this is a MIDIControl for a MIDI file player (rather than a MIDI-only device)
	 */
	private boolean isMIDIFilePlayer() {
		return handle != 0;
	}

	/**
	 * @return the handle to the player's handle (if MIDIControl for a MIDI
	 *         device only), or this control's handle, if a MIDIControl for a
	 *         MIDI file.
	 */
	private int getHandle() {
		if (isMIDIFilePlayer()) {
			return handle;
		}
		return player.handle;
	}

	/**
	 * Close the EAS interactive MIDI stream. Called by the owning Player when
	 * the player's native EAS handle is closed.
	 */
	void close() {
		if (handle != 0) {
			EAS.closeInteractiveMIDI(player.handle, handle);
			handle = 0;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#isBankQuerySupported()
	 */
	public boolean isBankQuerySupported() {
		return false;
	}

	private void notSupported() throws MediaException {
		throw new MediaException("not supported");
	}

	private void checkChannel(int channel) {
		if (channel < 0 || channel > 15) {
			throw new IllegalArgumentException("channel out of range");
		}
	}

	private void checkBank(int bank) {
		if (bank < 0 || bank > 16383) {
			throw new IllegalArgumentException("bank out of range");
		}
	}

	private void checkProgram(int program) {
		if (program < 0 || program > 127) {
			throw new IllegalArgumentException("program out of range");
		}
	}

	private void prerequisites() {
		player.mustNotBe(Player.REALIZED);
		player.mustNotBe(Player.CLOSED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#getProgram(int)
	 */
	public int[] getProgram(int channel) throws MediaException {
		prerequisites();
		checkChannel(channel);
		notSupported();
		return null; // satisfy compiler
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#getChannelVolume(int)
	 */
	public int getChannelVolume(int channel) {
		prerequisites();
		checkChannel(channel);
		return -1; // not supported
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#setProgram(int, int,
	 *      int)
	 */
	public void setProgram(int channel, int bank, int program) {
		prerequisites();
		checkChannel(channel);
		checkProgram(program);
		if (bank != -1) {
			checkBank(bank);
			shortMidiEvent(CONTROL_CHANGE | channel, 0x00, bank >> 7);
			shortMidiEvent(CONTROL_CHANGE | channel, 0x20, bank & 0x7F);
		}
		shortMidiEvent(0xC0 | channel, program, 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#setChannelVolume(int,
	 *      int)
	 */
	public void setChannelVolume(int channel, int volume) {
		prerequisites();
		checkChannel(channel);
		if (volume < 0 || volume > 127) {
			throw new IllegalArgumentException("channel volume out of range");
		}
		shortMidiEvent(CONTROL_CHANGE | channel, 0x07, volume);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#getBankList(boolean)
	 */
	public int[] getBankList(boolean custom) throws MediaException {
		prerequisites();
		notSupported();
		return null; // satisfy compiler
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#getProgramList(int)
	 */
	public int[] getProgramList(int bank) throws MediaException {
		prerequisites();
		checkBank(bank);
		notSupported();
		return null; // satisfy compiler
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#getProgramName(int,
	 *      int)
	 */
	public String getProgramName(int bank, int prog) throws MediaException {
		prerequisites();
		checkBank(bank);
		checkProgram(prog);
		notSupported();
		return null; // satisfy compiler
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#getKeyName(int, int,
	 *      int)
	 */
	public String getKeyName(int bank, int prog, int key) throws MediaException {
		prerequisites();
		checkBank(bank);
		checkProgram(prog);
		if (key < 0 || key > 127) {
			throw new IllegalArgumentException("key out of range");
		}
		notSupported();
		return null; // satisfy compiler
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#shortMidiEvent(int,
	 *      int, int)
	 */
	public void shortMidiEvent(int type, int data1, int data2) {
		prerequisites();
		if (type < 0x80 || type > 0xFF || data1 < 0 || data1 > 127 || data2 < 0
				|| data2 > 127) {
			throw new IllegalArgumentException(
					"shortMidiEvent parameter out of range");
		}
		// ignore sys ex and real time messages
		if ((type & 0xF0) == 0xF0) {
			return;
		}
		int len = 3;
		if ((type & 0xF0) == 0xC0 || (type & 0xF0) == 0xD0) {
			len = 2;
		}
		byte[] data = new byte[3];
		data[0] = (byte) type;
		data[1] = (byte) data1;
		data[2] = (byte) data2;

		try {
			EAS.writeMIDI(getHandle(), data, 0, len, isMIDIFilePlayer());
		} catch (MediaException me) {
			// nothing we can do
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.media.control.MIDIControl#longMidiEvent(byte[],
	 *      int, int)
	 */
	public int longMidiEvent(byte[] data, int offset, int length) {
		prerequisites();
		if (data == null || offset < 0 || offset + length > data.length
				|| length < 0) {
			throw new IllegalArgumentException(
					"longMidiEvent parameter out of range");
		}
		try {
			return EAS.writeMIDI(getHandle(), data, offset, length,
					isMIDIFilePlayer());
		} catch (MediaException me) {
			// nothing we can do
		}
		return -1;
	}

}
