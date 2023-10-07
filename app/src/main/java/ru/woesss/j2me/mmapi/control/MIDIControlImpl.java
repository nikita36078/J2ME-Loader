/*
 * Copyright 2023 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.woesss.j2me.mmapi.control;

import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.MIDIControl;

import ru.woesss.j2me.mmapi.synth.Library;

public class MIDIControlImpl implements MIDIControl {
	private final long handle; // = 0;

	private final Player player;
	private final Library library;

	public MIDIControlImpl(Player player, Library library, long playerHandle) {
		this.player = player;
		this.library = library;
		handle = playerHandle;
	}

	@Override
	public boolean isBankQuerySupported() {
		return false;
	}

	@Override
	public int[] getProgram(int channel) throws MediaException {
		checkRealized();
		checkChannel(channel);
		notSupported();
		return null; // satisfy compiler
	}

	@Override
	public int getChannelVolume(int channel) {
		checkRealized();
		checkChannel(channel);
		return -1; // not supported
	}

	@Override
	public void setProgram(int channel, int bank, int program) {
		checkRealized();
		checkChannel(channel);
		checkProgram(program);
		if (bank != -1) {
			checkBank(bank);
			shortMidiEvent(CONTROL_CHANGE | channel, 0x00, bank >> 7);
			shortMidiEvent(CONTROL_CHANGE | channel, 0x20, bank & 0x7F);
		}
		shortMidiEvent(0xC0 | channel, program, 0);
	}

	@Override
	public void setChannelVolume(int channel, int volume) {
		checkRealized();
		checkChannel(channel);
		if (volume < 0 || volume > 127) {
			throw new IllegalArgumentException("channel volume out of range");
		}
		shortMidiEvent(CONTROL_CHANGE | channel, 0x07, volume);

	}

	@Override
	public int[] getBankList(boolean custom) throws MediaException {
		checkRealized();
		notSupported();
		return null; // satisfy compiler
	}

	@Override
	public int[] getProgramList(int bank) throws MediaException {
		checkRealized();
		checkBank(bank);
		notSupported();
		return null; // satisfy compiler
	}

	@Override
	public String getProgramName(int bank, int prog) throws MediaException {
		checkRealized();
		checkBank(bank);
		checkProgram(prog);
		notSupported();
		return null; // satisfy compiler
	}

	@Override
	public String getKeyName(int bank, int prog, int key) throws MediaException {
		checkRealized();
		checkBank(bank);
		checkProgram(prog);
		if (key < 0 || key > 127) {
			throw new IllegalArgumentException("key out of range");
		}
		notSupported();
		return null; // satisfy compiler
	}

	@Override
	public void shortMidiEvent(int type, int data1, int data2) {
		checkRealized();
		if (type < 0x80 || type > 0xFF || data1 < 0 || data1 > 127 || data2 < 0 || data2 > 127) {
			throw new IllegalArgumentException("shortMidiEvent parameter out of range");
		}
		// ignore sys ex and real time messages
		if ((type & 0xF0) == 0xF0) {
			return;
		}
		byte[] data;
		if ((type & 0xF0) == 0xC0 || (type & 0xF0) == 0xD0) {
			data = new byte[]{(byte) type, (byte) data1};
		} else {
			data = new byte[] {(byte) type, (byte) data1, (byte) data2};
		}

		library.writeMIDI(handle, data, 0, data.length);
	}

	@Override
	public int longMidiEvent(byte[] data, int offset, int length) {
		checkRealized();
		if (data == null || offset < 0 || offset + length > data.length || length < 0) {
			throw new IllegalArgumentException("longMidiEvent parameter out of range");
		}
		return library.writeMIDI(handle, data, offset, length);
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

	private void checkRealized() {
		if (player.getState() < Player.REALIZED) {
			throw new IllegalStateException("call realize() before using the player");
		}
	}
}
