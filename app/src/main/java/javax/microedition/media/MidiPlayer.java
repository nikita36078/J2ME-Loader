/*
 * Copyright 2018 Nikita Shakarun
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

package javax.microedition.media;

import org.billthefarmer.mididriver.MidiConstants;
import org.billthefarmer.mididriver.MidiDriver;

import javax.microedition.media.control.MIDIControl;

public class MidiPlayer extends BasePlayer implements MIDIControl {

	private final MidiDriver midiDriver = MidiInterface.getDriver();

	public MidiPlayer() {
		addControl(MIDIControl.class.getName(), this);
	}

	@Override
	public int[] getBankList(boolean custom) {
		return new int[0];
	}

	@Override
	public int getChannelVolume(int channel) {
		return -1;
	}

	@Override
	public String getKeyName(int bank, int prog, int key) {
		return null;
	}

	@Override
	public int[] getProgram(int channel) {
		return new int[0];
	}

	@Override
	public int[] getProgramList(int bank) {
		return new int[0];
	}

	@Override
	public String getProgramName(int bank, int prog) {
		return "";
	}

	@Override
	public boolean isBankQuerySupported() {
		return false;
	}

	@Override
	public int longMidiEvent(byte[] data, int offset, int length) {
		if (midiDriver.write(data)) {
			return data.length;
		} else {
			return -1;
		}
	}

	@Override
	public void setChannelVolume(int channel, int volume) {
	}

	@Override
	public void setProgram(int channel, int bank, int program) {
		byte[] event = new byte[]{(byte) (MidiConstants.PROGRAM_CHANGE | channel), (byte) program};
		midiDriver.write(event);
	}

	@Override
	public void shortMidiEvent(int type, int data1, int data2) {
		byte[] event = new byte[]{(byte) type, (byte) data1, (byte) data2};
		midiDriver.write(event);
	}
}
