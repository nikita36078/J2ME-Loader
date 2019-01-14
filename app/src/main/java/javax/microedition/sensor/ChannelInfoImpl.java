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

public class ChannelInfoImpl implements ChannelInfo {
	private float accuracy;
	private int dataType;
	private MeasurementRange[] measurementRanges;
	private String name;
	private int scale;
	private Unit unit;

	public ChannelInfoImpl(float accuracy, int dataType, MeasurementRange[] measurementRanges,
						   String name, int scale, Unit unit) {
		this.accuracy = accuracy;
		this.dataType = dataType;
		this.measurementRanges = measurementRanges;
		this.name = name;
		this.scale = scale;
		this.unit = unit;
	}

	@Override
	public float getAccuracy() {
		return accuracy;
	}

	@Override
	public int getDataType() {
		return dataType;
	}

	@Override
	public MeasurementRange[] getMeasurementRanges() {
		return measurementRanges;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getScale() {
		return scale;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
}
