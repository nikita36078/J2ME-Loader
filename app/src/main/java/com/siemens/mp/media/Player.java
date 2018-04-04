/*
 *  Siemens API for MicroEmulator
 *  Copyright (C) 2003 Markus Heberling <markus@heberling.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */
package com.siemens.mp.media;

public interface Player extends Controllable {
	public static final int UNREALIZED = 100;
	public static final int REALIZED = 200;
	public static final int PREFETCHED = 300;
	public static final int STARTED = 400;
	public static final int CLOSED = 0;
	public static final long TIME_UNKNOWN = -1L;

	public abstract void realize() throws MediaException;

	public abstract void prefetch() throws MediaException;

	public abstract void start() throws MediaException;

	public abstract void stop() throws MediaException;

	public abstract void deallocate();

	public abstract void close();

	public abstract long setMediaTime(long l) throws MediaException;

	public abstract long getMediaTime();

	public abstract int getState();

	public abstract long getDuration();

	public abstract String getContentType();

	public abstract void setLoopCount(int i);

	public abstract void addPlayerListener(PlayerListener playerlistener);

	public abstract void removePlayerListener(PlayerListener playerlistener);
}
