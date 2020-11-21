/*
 * Copyright 2012 Kulikov Dmitriy
 * Copyright 2017-2018 Nikita Shakarun
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

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.amms.control.PanControl;
import javax.microedition.amms.control.audioeffect.EqualizerControl;
import javax.microedition.media.control.MetaDataControl;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.media.protocol.DataSource;

public class MicroPlayer extends BasePlayer implements MediaPlayer.OnCompletionListener,
		VolumeControl, PanControl {
	protected DataSource source;
	protected int state;
	private MediaPlayer player;
	private int loopCount;

	private ArrayList<PlayerListener> listeners;
	private HashMap<String, Control> controls;

	private boolean mute;
	private int level, pan;

	private InternalMetaData metadata;

	public MicroPlayer(DataSource datasource) {
		player = new AndroidPlayer();
		player.setOnCompletionListener(this);

		source = datasource;
		state = UNREALIZED;

		mute = false;
		level = 100;
		pan = 0;
		loopCount = 1;

		metadata = new InternalMetaData();
		InternalEqualizer equalizer = new InternalEqualizer();

		listeners = new ArrayList<>();
		controls = new HashMap<>();

		controls.put(VolumeControl.class.getName(), this);
		controls.put(PanControl.class.getName(), this);
		controls.put(MetaDataControl.class.getName(), metadata);
		controls.put(EqualizerControl.class.getName(), equalizer);
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

	public synchronized void postEvent(String event, Object eventData) {
		for (PlayerListener listener : listeners) {
			// Callbacks should be async
			Runnable r = () -> listener.playerUpdate(this, event, eventData);
			(new Thread(r, "MIDletPlayerCallback")).start();
		}
	}

	@Override
	public synchronized void onCompletion(MediaPlayer mp) {
		if (state == CLOSED) {
			return;
		}
		postEvent(PlayerListener.END_OF_MEDIA, new Long(getMediaTime()));

		if (loopCount == 1) {
			state = PREFETCHED;
			player.reset();
		} else if (loopCount > 1) {
			loopCount--;
		}

		if (state == STARTED && loopCount != -1) {
			player.start();
			postEvent(PlayerListener.STARTED, new Long(getMediaTime()));
		}
	}

	@Override
	public synchronized void realize() throws MediaException {
		checkClosed();

		if (state == UNREALIZED) {
			try {
				source.connect();
				player.setDataSource(source.getLocator());
			} catch (IOException e) {
				throw new MediaException(e.getMessage());
			}

			state = REALIZED;
		}
	}

	@Override
	public synchronized void prefetch() throws MediaException {
		checkClosed();

		if (state == UNREALIZED) {
			realize();
		}

		if (state == REALIZED) {
			try {
				MediaMetadataRetriever retriever = new MediaMetadataRetriever();
				retriever.setDataSource(source.getLocator());
				metadata.updateMetaData(retriever);
				retriever.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
			state = PREFETCHED;
		}
	}

	@Override
	public synchronized void start() throws MediaException {
		prefetch();

		if (state == PREFETCHED) {
			player.start();

			state = STARTED;
			postEvent(PlayerListener.STARTED, new Long(getMediaTime()));
		}
	}

	@Override
	public synchronized void stop() {
		checkClosed();
		if (state == STARTED) {
			player.pause();

			state = PREFETCHED;
			postEvent(PlayerListener.STOPPED, new Long(getMediaTime()));
		}
	}

	@Override
	public synchronized void deallocate() {
		stop();

		if (state == PREFETCHED) {
			player.reset();
			state = UNREALIZED;

			try {
				realize();
			} catch (MediaException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void close() {
		if (state != CLOSED) {
			player.release();
		}

		source.disconnect();

		state = CLOSED;
		postEvent(PlayerListener.CLOSED, null);
	}

	protected void checkClosed() {
		if (state == CLOSED) {
			throw new IllegalStateException("player is closed");
		}
	}

	protected void checkRealized() {
		checkClosed();

		if (state == UNREALIZED) {
			throw new IllegalStateException("call realize() before using the player");
		}
	}

	@Override
	public long setMediaTime(long now) throws MediaException {
		checkRealized();
		if (state < PREFETCHED) {
			return 0;
		} else {
			int time = (int) now / 1000;
			if (time != player.getCurrentPosition()) {
				player.seekTo(time);
			}
			return getMediaTime();
		}
	}

	@Override
	public long getMediaTime() {
		checkClosed();
		if (state < PREFETCHED) {
			return TIME_UNKNOWN;
		} else {
			return player.getCurrentPosition() * 1000;
		}
	}

	@Override
	public long getDuration() {
		checkClosed();
		if (state < PREFETCHED) {
			return TIME_UNKNOWN;
		} else {
			return player.getDuration() * 1000;
		}
	}

	@Override
	public void setLoopCount(int count) {
		checkClosed();
		if (state == STARTED)
			throw new IllegalStateException("player must not be in STARTED state while using setLoopCount()");

		if (count == 0) {
			throw new IllegalArgumentException("loop count must not be 0");
		}

		if (count == -1) {
			player.setLooping(true);
		} else {
			player.setLooping(false);
		}

		loopCount = count;
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public String getContentType() {
		checkRealized();
		return source.getContentType();
	}

	// VolumeControl

	private void updateVolume() {
		float left, right;

		if (mute) {
			left = right = 0;
		} else {
			if (level == 100) {
				left = right = 1.0f;
			} else {
				left = right = (float) (1 - (Math.log(100 - level) / Math.log(100)));
			}

			if (pan >= 0) {
				left *= (float) (100 - pan) / 100f;
			}

			if (pan < 0) {
				right *= (float) (100 + pan) / 100f;
			}
		}

		player.setVolume(left, right);
		postEvent(PlayerListener.VOLUME_CHANGED, this);
	}

	@Override
	public void setMute(boolean mute) {
		if (state == CLOSED) {
			// Avoid IllegalStateException in MediaPlayer.setVolume()
			return;
		}

		this.mute = mute;
		updateVolume();
	}

	@Override
	public boolean isMuted() {
		return mute;
	}

	@Override
	public int setLevel(int level) {
		if (state == CLOSED) {
			// Avoid IllegalStateException in MediaPlayer.setVolume()
			return this.level;
		}

		if (level < 0) {
			level = 0;
		} else if (level > 100) {
			level = 100;
		}

		this.level = level;
		updateVolume();

		return level;
	}

	@Override
	public int getLevel() {
		return level;
	}


	// PanControl

	@Override
	public int setPan(int pan) {
		if (pan < -100) {
			pan = -100;
		} else if (pan > 100) {
			pan = 100;
		}

		this.pan = pan;
		updateVolume();

		return pan;
	}

	@Override
	public int getPan() {
		return pan;
	}

}
