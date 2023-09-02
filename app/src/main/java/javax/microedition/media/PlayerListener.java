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
	String BUFFERING_STARTED = "bufferingStarted";
	String BUFFERING_STOPPED = "bufferingStopped";
	String CLOSED = "closed";
	String DEVICE_AVAILABLE = "deviceAvailable";
	String DEVICE_UNAVAILABLE = "deviceUnavailable";
	String DURATION_UPDATED = "durationUpdated";
	String END_OF_MEDIA = "endOfMedia";
	String ERROR = "error";
	String STARTED = "started";
	String STOPPED = "stopped";
	String VOLUME_CHANGED = "volumeChanged";
	String RECORD_STARTED = "recordStarted";
	String RECORD_STOPPED = "recordStopped";
	String RECORD_ERROR = "recordError";
	String SIZE_CHANGED = "sizeChanged";
	String STOPPED_AT_TIME = "stoppedAtTime";

	void playerUpdate(Player player, String event, Object eventData);
}
