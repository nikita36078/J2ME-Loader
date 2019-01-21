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

import java.util.ArrayList;
import java.util.Hashtable;

public final class SensorManager {
	private static final String SCHEME_CONTEXT = "contextType";
	private static final String SCHEME_LOCATION = "location";
	private static final String SCHEME_MODEL = "model";
	private static final String SCHEME_QUERY_SEP = "?";
	private static final String SCHEME_SENSOR = "sensor:";
	private static final char SCHEME_COLON = ':';
	private static final char SCHEME_SEMICOLON = ';';
	private static final char SCHEME_EQUALS = '=';

	private static final int ACCELEROMETER_SCALE = 1000;
	private static final MeasurementRange[] accelerometerMeasurementRanges = new MeasurementRange[]{
			new MeasurementRange(-9.8 * ACCELEROMETER_SCALE, 9.8 * ACCELEROMETER_SCALE, 0.01 * ACCELEROMETER_SCALE)
	};
	private static final ChannelInfo[] accelerometerChannelInfo = new ChannelInfo[]{
			new ChannelInfoImpl(-1.0f, ChannelInfo.TYPE_INT, accelerometerMeasurementRanges, "axis_x", ACCELEROMETER_SCALE, Unit.getUnit("m/s^2")),
			new ChannelInfoImpl(-1.0f, ChannelInfo.TYPE_INT, accelerometerMeasurementRanges, "axis_y", ACCELEROMETER_SCALE, Unit.getUnit("m/s^2")),
			new ChannelInfoImpl(-1.0f, ChannelInfo.TYPE_INT, accelerometerMeasurementRanges, "axis_z", ACCELEROMETER_SCALE, Unit.getUnit("m/s^2")),
	};
	private static final SensorInfo accelerometerSensorInfo = new SensorInfoImpl(accelerometerChannelInfo, SensorInfo.CONN_EMBEDDED,
			SensorInfo.CONTEXT_TYPE_USER, "Accelerometer data", "default", "acceleration");
	private static final SensorInfo[] sensorInfos = new SensorInfo[]{accelerometerSensorInfo};

	public static void addSensorListener(SensorListener listener, String str) {
	}

	public static void addSensorListener(SensorListener listener, SensorInfo info) {
	}

	public static SensorInfo[] findSensors(String url) {
		if (url.startsWith(SCHEME_SENSOR)) {
			url = url.substring(SCHEME_SENSOR.length());
		} else {
			throw new IllegalArgumentException();
		}
		if (url.indexOf(SCHEME_COLON) > -1) {
			throw new IllegalArgumentException();
		}
		boolean isOnlyQuantity = false;
		int start = 0;
		int end = 0;

		end = url.indexOf(SCHEME_SEMICOLON, start);
		if (end < 0)
			end = url.indexOf(SCHEME_QUERY_SEP, start);
		if (end < 0) {
			end = url.length();
			isOnlyQuantity = true;
		}

		String quantity = url.substring(start, end);

		if (quantity.length() == 0) {
			throw new IllegalArgumentException();
		}

		String contextType = null;
		String location = null;
		String model = null;

		if (!isOnlyQuantity) {
			url = url.substring(quantity.length() + 1);
			Hashtable properties = parseProperty(url);

			contextType = (String) properties.remove(SCHEME_CONTEXT);
			if (contextType != null && !isValidContext(contextType)) {
				throw new IllegalArgumentException();
			}
			location = (String) properties.remove(SCHEME_LOCATION);
			model = (String) properties.remove(SCHEME_MODEL);
			if (properties.size() != 0) {
				throw new IllegalArgumentException();
			}
		}

		ArrayList<SensorInfo> matchingInfos = new ArrayList<>();

		for (SensorInfo sensorInfo : sensorInfos) {
			if (!sensorInfo.getQuantity().equals(quantity)) {
				continue;
			}
			if (contextType != null && !contextType.equals(sensorInfo.getContextType())) {
				continue;
			}
			if (model != null && !model.equals(sensorInfo.getModel())) {
				continue;
			}
			if (location != null) {
				Object sensorLoc = null;
				try {
					sensorLoc = sensorInfo.getProperty(SensorInfo.PROP_LOCATION);
				} catch (IllegalArgumentException iae) {
					// sensorinfo did not have location property
				}
				if (!location.equals(sensorLoc)) {
					continue;
				}
			}
			matchingInfos.add(sensorInfo);
		}
		return matchingInfos.toArray(new SensorInfo[0]);

	}

	public static SensorInfo[] findSensors(String quantity, String contextType) {
		if (quantity == null && contextType == null) {
			return sensorInfos;
		}
		if (quantity == null) {
			if (isValidContext(contextType)) {
				ArrayList<SensorInfo> matchingInfos = new ArrayList<>();
				for (SensorInfo sensorInfo : sensorInfos) {
					if (sensorInfo.getContextType().equals(contextType)) {
						matchingInfos.add(sensorInfo);
					}
				}
				return matchingInfos.toArray(new SensorInfo[0]);
			} else {
				throw new IllegalArgumentException();
			}
		} else {
			String url = SCHEME_SENSOR + quantity;
			if (contextType != null) {
				url += SCHEME_SEMICOLON + SCHEME_CONTEXT + SCHEME_EQUALS + contextType;
			}
			return findSensors(url);
		}
	}

	public static void removeSensorListener(SensorListener listener) {
	}

	private static boolean isValidContext(String contextType) {
		return contextType.equals(SensorInfo.CONTEXT_TYPE_AMBIENT)
				|| contextType.equals(SensorInfo.CONTEXT_TYPE_DEVICE)
				|| contextType.equals(SensorInfo.CONTEXT_TYPE_USER)
				|| contextType.equals(SensorInfo.CONTEXT_TYPE_VEHICLE);
	}

	private static Hashtable parseProperty(String properties) {
		Hashtable<String, String> result = new Hashtable<>();
		int start = 0;
		int end = -1;
		int length = properties.length();
		while (start >= 0 && start < length) {
			end = properties.indexOf(SCHEME_EQUALS, start);
			if (-1 == end) {
				throw new IllegalArgumentException();
			}
			String key = properties.substring(start, end);

			start = end + 1;
			end = properties.indexOf(SCHEME_SEMICOLON, start);
			if (-1 == end && start < length) {
				end = length;
			}
			if (-1 == end) {
				throw new IllegalArgumentException();
			}
			String value = properties.substring(start, end);
			result.put(key, value);
			start = end + 1;
		}
		return result;
	}

}
