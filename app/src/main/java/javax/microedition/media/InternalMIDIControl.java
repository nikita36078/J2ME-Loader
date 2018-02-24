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
