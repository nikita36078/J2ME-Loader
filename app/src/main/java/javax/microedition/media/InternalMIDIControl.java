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

import javax.microedition.media.control.MIDIControl;

public class InternalMIDIControl implements MIDIControl {
	@Override
	public int[] getBankList(boolean custom) {
		return new int[0];
	}

	@Override
	public int getChannelVolume(int channel) {
		return 0;
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
		return null;
	}

	@Override
	public boolean isBankQuerySupported() {
		return false;
	}

	@Override
	public int longMidiEvent(byte[] data, int offset, int length) {
		return 0;
	}

	@Override
	public void setChannelVolume(int channel, int volume) {

	}

	@Override
	public void setProgram(int channel, int bank, int program) {

	}

	@Override
	public void shortMidiEvent(int type, int data1, int data2) {

	}
}
