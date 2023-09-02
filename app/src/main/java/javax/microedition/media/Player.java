/*
 * Copyright 2012 Kulikov Dmitriy
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

package javax.microedition.media;

public interface Player extends Controllable {
	int CLOSED = 0;
	int UNREALIZED = 100;
	int REALIZED = 200;
	int PREFETCHED = 300;
	int STARTED = 400;

	long TIME_UNKNOWN = -1;

	void realize() throws MediaException;

	void prefetch() throws MediaException;

	void start() throws MediaException;

	void stop() throws MediaException;

	void deallocate();

	void close();

	long setMediaTime(long now) throws MediaException;

	long getMediaTime();

	TimeBase getTimeBase();

	void setTimeBase(TimeBase master) throws MediaException;

	long getDuration();

	void setLoopCount(int count);

	int getState();

	void addPlayerListener(PlayerListener playerListener);

	void removePlayerListener(PlayerListener playerListener);

	String getContentType();
}
