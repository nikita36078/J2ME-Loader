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

package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface MIDIControl extends Control {
	public static final int CONTROL_CHANGE = 176;
	public static final int NOTE_ON = 144;

	public int[] getBankList(boolean custom);

	public int getChannelVolume(int channel);

	public String getKeyName(int bank, int prog, int key);

	public int[] getProgram(int channel);

	public int[] getProgramList(int bank);

	public String getProgramName(int bank, int prog);

	public boolean isBankQuerySupported();

	public int longMidiEvent(byte[] data, int offset, int length);

	public void setChannelVolume(int channel, int volume);

	public void setProgram(int channel, int bank, int program);

	public void shortMidiEvent(int type, int data1, int data2);
}
