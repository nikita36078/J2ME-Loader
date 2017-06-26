/*
 * Copyright 2012 Kulikov Dmitriy
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

package javax.microedition.media;

public interface PlayerListener {
	public static final String CLOSED = "closed";
	public static final String DEVICE_AVAILABLE = "deviceAvailable";
	public static final String DEVICE_UNAVAILABLE = "deviceUnavailable";
	public static final String DURATION_UPDATED = "durationUpdated";
	public static final String END_OF_MEDIA = "endOfMedia";
	public static final String ERROR = "error";
	public static final String STARTED = "started";
	public static final String STOPPED = "stopped";
	public static final String VOLUME_CHANGED = "volumeChanged";

	public void playerUpdate(Player player, String event, Object eventData);
}