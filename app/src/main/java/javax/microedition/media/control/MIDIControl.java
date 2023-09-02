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
import javax.microedition.media.MediaException;

public interface MIDIControl extends Control {
	int CONTROL_CHANGE = 176;
	int NOTE_ON = 144;

	int[] getBankList(boolean custom) throws MediaException;

	int getChannelVolume(int channel);

	String getKeyName(int bank, int prog, int key) throws MediaException;

	int[] getProgram(int channel) throws MediaException;

	int[] getProgramList(int bank) throws MediaException;

	String getProgramName(int bank, int prog) throws MediaException;

	boolean isBankQuerySupported();

	int longMidiEvent(byte[] data, int offset, int length);

	void setChannelVolume(int channel, int volume);

	void setProgram(int channel, int bank, int program);

	void shortMidiEvent(int type, int data1, int data2);
}
