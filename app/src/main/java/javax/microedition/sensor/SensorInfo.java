/*
 * J2ME Loader
 * Copyright (C) 2017 Nikita Shakarun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

	ChannelInfo[] getChannelInfos();

	int getConnectionType();

	String getContextType();

	String getDescription();

	int getMaxBufferSize();

	String getModel();

	Object getProperty(String str);

	String[] getPropertyNames();

	String getQuantity();

	String getUrl();

	boolean isAvailabilityPushSupported();

	boolean isAvailable();

	boolean isConditionPushSupported();
}
