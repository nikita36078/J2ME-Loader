/*
 *  Copyright 2023 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.woesss.j2me.mmapi.sonivox;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.amms.control.PanControl;
import javax.microedition.amms.control.audioeffect.EqualizerControl;
import javax.microedition.media.BasePlayer;
import javax.microedition.media.Control;
import javax.microedition.media.InternalDataSource;
import javax.microedition.media.InternalEqualizer;
import javax.microedition.media.InternalMetaData;
import javax.microedition.media.MediaException;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.MetaDataControl;
import javax.microedition.media.control.VolumeControl;

public class PlayerEAS extends BasePlayer implements VolumeControl, PanControl {
	private static final String TAG = "PlayerEAS";
	private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor(r ->
			new Thread(r, "MidletPlayerCallback"));
	private final ArrayList<PlayerListener> listeners = new ArrayList<>();
	private final HashMap<String, Control> controls = new HashMap<>();
	private final InternalMetaData metadata = new InternalMetaData();
	private final InternalDataSource dataSource;
	private final long handle;
	private long duration = TIME_UNKNOWN;

	private int state = UNREALIZED;

	public PlayerEAS(InternalDataSource dataSource) {
		handle = EAS.playerInit(dataSource.getLocator());
		this.dataSource = dataSource;
		controls.put(VolumeControl.class.getName(), this);
		controls.put(PanControl.class.getName(), this);
		controls.put(MetaDataControl.class.getName(), metadata);
		controls.put(EqualizerControl.class.getName(), new InternalEqualizer());
		EAS.playerListener(handle, this);
	}

	@Override
	public void realize() throws MediaException {
		checkClosed();

		if (state == UNREALIZED) {
			try {
				dataSource.connect();
				EAS.playerRealize(handle, dataSource.getLocator());
			} catch (IOException e) {
				throw new MediaException(e);
			}

			state = REALIZED;
		}
	}

	@Override
	public void prefetch() throws MediaException {
		checkClosed();

		if (state == UNREALIZED) {
			realize();
		}

		if (state == REALIZED) {
			try {
				MediaMetadataRetriever retriever = new MediaMetadataRetriever();
				retriever.setDataSource(dataSource.getLocator());
				metadata.updateMetaData(retriever);
				retriever.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
			duration = EAS.playerPrefetch(handle);
			state = PREFETCHED;
		}
	}

	@Override
	public void start() throws MediaException {
		prefetch();

		if (state == PREFETCHED) {
			EAS.playerStart(handle);

			state = STARTED;
			postEvent(PlayerListener.STARTED, getMediaTime());
		}
	}

	@Override
	public void stop() throws MediaException {
		checkClosed();
		if (state == STARTED) {
			EAS.playerPause(handle);

			state = PREFETCHED;
			postEvent(PlayerListener.STOPPED, getMediaTime());
		}
	}

	@Override
	public void deallocate() {
		try {
			stop();
		} catch (MediaException e) {
			Log.e(TAG, "deallocate: stop() failed", e);
		}

		if (state == PREFETCHED) {
			EAS.playerDeallocate(handle);
			state = UNREALIZED;
		}
	}

	@Override
	public void close() {
		if (state != CLOSED) {
			EAS.playerClose(handle);
		}

		dataSource.disconnect();

		state = CLOSED;
		postEvent(PlayerListener.CLOSED, null);
	}

	@Override
	public long setMediaTime(long now) throws MediaException {
		checkRealized();
		if (state < PREFETCHED) {
			return 0;
		} else {
			return EAS.setMediaTime(handle, now);
		}
	}

	@Override
	public long getMediaTime() {
		checkClosed();
		if (state < PREFETCHED) {
			return TIME_UNKNOWN;
		} else {
			return EAS.getMediaTime(handle);
		}
	}

	@Override
	public long getDuration() {
		return duration;
	}

	@Override
	public void setLoopCount(int count) {
		checkClosed();
		if (state == STARTED)
			throw new IllegalStateException("player must not be in STARTED state while using setLoopCount()");

		if (count == 0) {
			throw new IllegalArgumentException("loop count must not be 0");
		}
		EAS.setRepeat(handle, count);
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public String getContentType() {
		checkRealized();
		return dataSource.getContentType();
	}

	@Override
	public int setPan(int pan) {
		return EAS.setPan(handle, pan);
	}

	@Override
	public int getPan() {
		return EAS.getPan(handle);
	}

	@Override
	public void setMute(boolean mute) {
		EAS.setMute(handle, mute);

	}

	@Override
	public boolean isMuted() {
		return EAS.isMuted(handle);
	}

	@Override
	public int setLevel(int level) {
		return EAS.setVolume(handle, level);
	}

	@Override
	public int getLevel() {
		return EAS.getVolume(handle);
	}

	@Override
	public Control getControl(String controlType) {
		checkRealized();
		if (!controlType.contains(".")) {
			controlType = "javax.microedition.media.control." + controlType;
		}
		return controls.get(controlType);
	}

	@Override
	public Control[] getControls() {
		checkRealized();
		return controls.values().toArray(new Control[0]);
	}

	@Override
	public synchronized void addPlayerListener(PlayerListener playerListener) {
		checkClosed();
		if (!listeners.contains(playerListener) && playerListener != null) {
			listeners.add(playerListener);
		}
	}

	@Override
	public synchronized void removePlayerListener(PlayerListener playerListener) {
		checkClosed();
		listeners.remove(playerListener);
	}

	/** @noinspection unused */
	@Keep // call from native
	private void postEvent(int type, long time) {
		switch (type) {
			case 1: // endOfMedia
				postEvent(PlayerListener.END_OF_MEDIA, time);
				break;
			case 2: // started
				postEvent(PlayerListener.STARTED, time);
				break;
			case 3: // error
				postEvent(PlayerListener.ERROR, null);
				break;
		}
	}

	private synchronized void postEvent(String event, Object eventData) {
		for (PlayerListener listener : listeners) {
			// Callbacks should be async
			callbackExecutor.execute(() -> listener.playerUpdate(this, event, eventData));
		}
	}

	private void checkClosed() {
		if (state == CLOSED) {
			throw new IllegalStateException("player is closed");
		}
	}

	private void checkRealized() {
		checkClosed();

		if (state == UNREALIZED) {
			throw new IllegalStateException("call realize() before using the player");
		}
	}

	@Override
	protected void finalize() throws Throwable {
		EAS.playerFinalize(handle);
	}
}
