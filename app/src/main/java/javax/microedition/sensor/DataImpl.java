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

package javax.microedition.sensor;

public class DataImpl implements Data {
	private final ChannelInfo channelInfo;
	private double[] doubleValues;
	private int[] intValues;
	private Object[] objectValues;
	private long[] timestamps;

	public DataImpl(ChannelInfo channelInfo) {
		this.channelInfo = channelInfo;
	}

	public DataImpl(ChannelInfo channelInfo, double[] doubleValues, long[] timestamps) {
		this.channelInfo = channelInfo;
		this.doubleValues = doubleValues;
		this.timestamps = timestamps;
	}

	public DataImpl(ChannelInfo channelInfo, int[] intValues, long[] timestamps) {
		this.channelInfo = channelInfo;
		this.intValues = intValues;
		this.timestamps = timestamps;
	}

	public DataImpl(ChannelInfo channelInfo, Object[] objectValues, long[] timestamps) {
		this.channelInfo = channelInfo;
		this.objectValues = objectValues;
		this.timestamps = timestamps;
	}

	@Override
	public ChannelInfo getChannelInfo() {
		return channelInfo;
	}

	@Override
	public double[] getDoubleValues() {
		if (doubleValues != null) {
			return doubleValues;
		}

		// Compatibility handling: convert int and object to double
		switch (channelInfo.getDataType()) {
			case ChannelInfo.TYPE_DOUBLE:
				return doubleValues;

			case ChannelInfo.TYPE_INT:
				if (intValues != null) {
					doubleValues = new double[intValues.length];
					for (int i = 0; i < intValues.length; i++) {
						doubleValues[i] = intValues[i] * 0.001; // prevent multiplying by 100
					}
					return doubleValues;
				}
				break;

			case ChannelInfo.TYPE_OBJECT:
				if (objectValues != null) {
					doubleValues = new double[objectValues.length];
					for (int i = 0; i < objectValues.length; i++) {
						Object v = objectValues[i];
						if (v instanceof Number) {
							doubleValues[i] = ((Number) v).doubleValue();
						} else {
							throw new IllegalStateException();
						}
					}
					return doubleValues;
				}
				break;
		}

		// Default fallback
		throw new IllegalStateException();
	}

	@Override
	public int[] getIntValues() {
		if (channelInfo.getDataType() != ChannelInfo.TYPE_INT) {
			throw new IllegalStateException();
		}
		return intValues;
	}

	@Override
	public Object[] getObjectValues() {
		if (channelInfo.getDataType() != ChannelInfo.TYPE_OBJECT) {
			throw new IllegalStateException();
		}
		return objectValues;
	}

	@Override
	public long getTimestamp(int index) {
		if (timestamps == null) {
			throw new IllegalStateException();
		}
		return timestamps[index];
	}

	@Override
	public float getUncertainty(int index) {
		return 0;
	}

	@Override
	public boolean isValid(int index) {
		return true;
	}
}
