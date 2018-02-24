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
