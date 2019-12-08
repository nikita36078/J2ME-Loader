/*
 * Copyright 2017 Nikita Shakarun
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

package javax.microedition.sensor;

import java.io.IOException;

import javax.microedition.io.Connection;

public interface SensorConnection extends Connection {
	public static final int STATE_CLOSED = 4;
	public static final int STATE_LISTENING = 2;
	public static final int STATE_OPENED = 1;

	public Channel getChannel(ChannelInfo channelInfo);

	public Data[] getData(int bufferSize) throws IOException;

	public Data[] getData(int bufferSize, long bufferingPeriod, boolean isTimestampIncluded,
						  boolean isUncertaintyIncluded, boolean isValidityIncluded) throws IOException;

	public int[] getErrorCodes();

	public String getErrorText(int bufferSize);

	public SensorInfo getSensorInfo();

	public int getState();

	public void removeDataListener();

	public void setDataListener(DataListener listener, int bufferSize);

	public void setDataListener(DataListener listener, int bufferSize, long bufferingPeriod, boolean isTimestampIncluded,
								boolean isUncertaintyIncluded, boolean isValidityIncluded);
}
