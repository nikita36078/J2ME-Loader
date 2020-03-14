/*
 * Copyright 2019 Nikita Shakarun
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

package org.microemu.cldc.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.microemu.microedition.io.ConnectionImplementation;

import java.io.IOException;
import java.util.Arrays;

import javax.microedition.sensor.AndroidSensorManager;
import javax.microedition.sensor.Channel;
import javax.microedition.sensor.ChannelImpl;
import javax.microedition.sensor.ChannelInfo;
import javax.microedition.sensor.Data;
import javax.microedition.sensor.DataImpl;
import javax.microedition.sensor.DataListener;
import javax.microedition.sensor.SensorConnection;
import javax.microedition.sensor.SensorInfo;
import javax.microedition.sensor.SensorManager;
import javax.microedition.util.ContextHolder;

public class Connection implements SensorConnection, ConnectionImplementation, SensorEventListener {

	private SensorInfo sensorInfo;
	private ChannelInfo[] channelInfos;
	private DataListener listener;
	private int bufferSize;
	private int eventsNum;
	private int state;
	private int dataLength;
	private Data[] data;
	private Channel[] channels;
	private long[] timestamps;

	private android.hardware.SensorManager sensorManager;
	private Sensor sensor;

	@Override
	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		SensorInfo[] sensorInfos = SensorManager.findSensors(name);
		if (sensorInfos.length < 1) {
			throw new IllegalArgumentException();
		}
		sensorInfo = sensorInfos[0];
		channelInfos = sensorInfo.getChannelInfos();
		dataLength = sensorInfo.getChannelInfos().length;
		data = new Data[dataLength];
		channels = new Channel[dataLength];
		for (int i = 0; i < dataLength; i++) {
			data[i] = new DataImpl(channelInfos[i]);
			channels[i] = new ChannelImpl(channelInfos[i]);
		}
		timestamps = new long[dataLength];

		sensorManager = (android.hardware.SensorManager) ContextHolder.getAppContext().getSystemService(Context.SENSOR_SERVICE);
		int type = AndroidSensorManager.getSensorType(sensorInfo.getQuantity());
		sensor = sensorManager.getDefaultSensor(type);
		state = STATE_OPENED;
		return this;
	}

	@Override
	public Channel getChannel(ChannelInfo channelInfo) {
		for (int i = 0; i < dataLength; i++) {
			if (channelInfos[i] == channelInfo) {
				return channels[i];
			}
		}
		throw new IllegalArgumentException();
	}

	@Override
	public Data[] getData(int bufferSize) throws IOException {
		return getData(bufferSize, -1, false, false, false);
	}

	@Override
	public Data[] getData(int bufferSize, long bufferingPeriod, boolean isTimestampIncluded,
						  boolean isUncertaintyIncluded, boolean isValidityIncluded) throws IOException {
		if (state == STATE_CLOSED) {
			throw new IOException();
		}
		if (state == STATE_LISTENING) {
			throw new IllegalStateException();
		}
		return data;
	}

	@Override
	public int[] getErrorCodes() {
		return new int[0];
	}

	@Override
	public String getErrorText(int bufferSize) {
		throw new IllegalArgumentException();
	}

	@Override
	public SensorInfo getSensorInfo() {
		return sensorInfo;
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public void removeDataListener() {
		if (state == STATE_CLOSED) {
			throw new IllegalStateException();
		}
		listener = null;
		sensorManager.unregisterListener(this);
		state = STATE_OPENED;
	}

	@Override
	public void setDataListener(DataListener listener, int bufferSize) {
		setDataListener(listener, bufferSize, -1, false, false, false);
	}

	@Override
	public void setDataListener(DataListener listener, int bufferSize, long bufferingPeriod, boolean isTimestampIncluded, boolean isUncertaintyIncluded, boolean isValidityIncluded) {
		if (state == STATE_CLOSED) {
			throw new IllegalStateException();
		}
		if ((bufferSize < 1 && bufferingPeriod < 1) || bufferSize > sensorInfo.getMaxBufferSize()) {
			throw new IllegalArgumentException();
		}
		if (bufferSize < 1) {
			bufferSize = sensorInfo.getMaxBufferSize();
		}
		this.listener = listener;
		this.bufferSize = bufferSize;
		sensorManager.registerListener(this, sensor, android.hardware.SensorManager.SENSOR_DELAY_GAME);
		state = STATE_LISTENING;
	}

	@Override
	public void close() throws IOException {
		state = STATE_CLOSED;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (listener != null) {
			eventsNum++;
			if (eventsNum >= bufferSize) {
				Arrays.fill(timestamps, event.timestamp);
				for (int i = 0; i < dataLength; i++) {
					float value = event.values[i] * channelInfos[i].getScale();
					if (channelInfos[i].getDataType() == ChannelInfo.TYPE_DOUBLE) {
						data[i] = new DataImpl(channelInfos[i], new double[]{value}, timestamps);
					} else if (channelInfos[i].getDataType() == ChannelInfo.TYPE_INT) {
						data[i] = new DataImpl(channelInfos[i], new int[]{(int) value}, timestamps);
					} else {
						data[i] = new DataImpl(channelInfos[i], new Object[]{value}, timestamps);
					}
				}
				try {
					listener.dataReceived(this, data, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
				eventsNum = 0;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}
}
