/*
 * Copyright 2018 Nikita Shakarun
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

public class BasePlayer implements Player {
	private TimeBase timeBase;

	@Override
	public void realize() throws MediaException {
	}

	@Override
	public void prefetch() throws MediaException {
	}

	@Override
	public void start() throws MediaException {
	}

	@Override
	public void stop() throws MediaException {
	}

	@Override
	public void deallocate() {
	}

	@Override
	public void close() {
	}

	@Override
	public long setMediaTime(long now) throws MediaException {
		return 0;
	}

	@Override
	public long getMediaTime() {
		return 0;
	}

	@Override
	public TimeBase getTimeBase() {
		if (timeBase == null) {
			return Manager.getSystemTimeBase();
		}
		return timeBase;
	}

	@Override
	public void setTimeBase(TimeBase master) throws MediaException {
		timeBase = master;
	}

	@Override
	public long getDuration() {
		return 0;
	}

	@Override
	public void setLoopCount(int count) {
	}

	@Override
	public int getState() {
		return 0;
	}

	@Override
	public void addPlayerListener(PlayerListener playerListener) {
	}

	@Override
	public void removePlayerListener(PlayerListener playerListener) {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public Control getControl(String controlType) {
		return null;
	}

	@Override
	public Control[] getControls() {
		return new Control[0];
	}
}
