package javax.microedition.sensor;

import javax.microedition.io.Connection;

public interface SensorConnection extends Connection {
	public static final int STATE_CLOSED = 4;
	public static final int STATE_LISTENING = 2;
	public static final int STATE_OPENED = 1;

	Channel getChannel(ChannelInfo channelInfo);

	Data[] getData(int i);

	Data[] getData(int i, long j, boolean z, boolean z2, boolean z3);

	int[] getErrorCodes();

	String getErrorText(int i);

	SensorInfo getSensorInfo();

	int getState();

	void removeDataListener();

	void setDataListener(DataListener dataListener, int i);

	void setDataListener(DataListener dataListener, int i, long j, boolean z, boolean z2, boolean z3);
}
