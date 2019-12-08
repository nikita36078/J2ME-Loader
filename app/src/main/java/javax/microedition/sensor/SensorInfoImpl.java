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

public class SensorInfoImpl implements SensorInfo {
	private ChannelInfo[] channelInfos;
	private int connectionType;
	private String contextType;
	private String description;
	private String model;
	private String quantity;

	public SensorInfoImpl(ChannelInfo[] channelInfos, int connectionType, String contextType,
						  String description, String model, String quantity) {
		this.channelInfos = channelInfos;
		this.connectionType = connectionType;
		this.contextType = contextType;
		this.description = description;
		this.model = model;
		this.quantity = quantity;
	}

	@Override
	public ChannelInfo[] getChannelInfos() {
		return channelInfos;
	}

	@Override
	public int getConnectionType() {
		return connectionType;
	}

	@Override
	public String getContextType() {
		return contextType;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getMaxBufferSize() {
		return 1024;
	}

	@Override
	public String getModel() {
		return model;
	}

	@Override
	public Object getProperty(String name) {
		throw new IllegalArgumentException();
	}

	@Override
	public String[] getPropertyNames() {
		return new String[0];
	}

	@Override
	public String getQuantity() {
		return quantity;
	}

	@Override
	public String getUrl() {
		return "sensor:" + quantity;
	}

	@Override
	public boolean isAvailabilityPushSupported() {
		return false;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean isConditionPushSupported() {
		return false;
	}
}
