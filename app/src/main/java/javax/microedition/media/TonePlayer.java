/*
 * Copyright 2020 Nikita Shakarun
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

import org.billthefarmer.mididriver.MidiDriver;

import java.util.HashMap;

import javax.microedition.media.control.ToneControl;
import javax.microedition.media.tone.ToneSequence;

public class TonePlayer extends BasePlayer implements ToneControl {

	private static final byte[] EMPTY_MIDI_SEQUENCE = {
			0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x10,
			0x4D, 0x54, 0x72, 0x6B, 0x00, 0x00, 0x00, 0x12, 0x00, (byte) 0xFF, 0x51, 0x03,
			0x07, (byte) 0xA1, 0x20, 0x00, (byte) 0xC0, 0x01, 0x00, (byte) 0x80, 0x40,
			0x7F, 0x00, (byte) 0xFF, 0x2F, 0x00
	};
	private final HashMap<String, Control> controls = new HashMap<>();
	private final MidiDriver midiDriver = MidiInterface.getDriver();
	private byte[] midiSequence = EMPTY_MIDI_SEQUENCE;
	private long duration;

	public TonePlayer() {
		controls.put(ToneControl.class.getName(), this);
	}

	@Override
	public Control getControl(String controlType) {
		if (!controlType.contains(".")) {
			controlType = "javax.microedition.media.control." + controlType;
		}
		return controls.get(controlType);
	}

	@Override
	public Control[] getControls() {
		return controls.values().toArray(new Control[0]);
	}

	@Override
	public void setSequence(byte[] sequence) {
		try {
			ToneSequence tone = new ToneSequence(sequence);
			tone.process();
			midiSequence = tone.getByteArray();
			duration = tone.getDuration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public long getDuration() {
		return duration;
	}

	@Override
	public void start() throws MediaException {
		midiDriver.write(midiSequence);
	}

	@Override
	public void deallocate() {}

	@Override
	public void close() {
		deallocate();
	}
}
