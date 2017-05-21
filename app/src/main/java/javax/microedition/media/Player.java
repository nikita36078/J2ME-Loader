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

public interface Player extends Controllable
{
	public static final int CLOSED = 0;
	public static final int UNREALIZED = 100;
	public static final int REALIZED = 200;
	public static final int PREFETCHED = 300;
	public static final int STARTED = 400;
	
	public static final long TIME_UNKNOWN = -1;
	
	public void realize() throws MediaException;
	public void prefetch() throws MediaException;
	public void start() throws MediaException;
	public void stop() throws MediaException;
	public void deallocate();
	public void close();
	
	public long setMediaTime(long now) throws MediaException;
	public long getMediaTime();
	public long getDuration();
	public void setLoopCount(int count);
	
	public int getState();
	
	public void addPlayerListener(PlayerListener playerListener);
	public void removePlayerListener(PlayerListener playerListener);
}