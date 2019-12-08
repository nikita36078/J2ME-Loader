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

public interface SensorInfo {
	public static final int CONN_EMBEDDED = 1;
	public static final int CONN_REMOTE = 2;
	public static final int CONN_SHORT_RANGE_WIRELESS = 4;
	public static final int CONN_WIRED = 8;
	public static final String CONTEXT_TYPE_AMBIENT = "ambient";
	public static final String CONTEXT_TYPE_DEVICE = "device";
	public static final String CONTEXT_TYPE_USER = "user";
	public static final String CONTEXT_TYPE_VEHICLE = "vehicle";
	public static final String PROP_IS_CONTROLLABLE = "controllable";
	public static final String PROP_IS_REPORTING_ERRORS = "errorsReported";
	public static final String PROP_LATITUDE = "latitude";
	public static final String PROP_LOCATION = "location";
	public static final String PROP_LONGITUDE = "longitude";
	public static final String PROP_MAX_RATE = "maxSamplingRate";
	public static final String PROP_VENDOR = "vendor";
	public static final String PROP_VERSION = "version";

	public ChannelInfo[] getChannelInfos();

	public int getConnectionType();

	public String getContextType();

	public String getDescription();

	public int getMaxBufferSize();

	public String getModel();

	public Object getProperty(String name);

	public String[] getPropertyNames();

	public String getQuantity();

	public String getUrl();

	public boolean isAvailabilityPushSupported();

	public boolean isAvailable();

	public boolean isConditionPushSupported();
}
