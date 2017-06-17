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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.amms.control.audioeffect.EqualizerControl;
import javax.microedition.media.control.MetaDataControl;
import javax.microedition.media.control.PanControl;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.media.protocol.DataSource;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;

public class MicroPlayer implements Player, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, VolumeControl, PanControl
{
	protected DataSource source;
	protected MediaPlayer player;
	protected int state;
	protected int loopCount;
	
	protected final Object lock = new Object();
	
	protected ArrayList<PlayerListener> listeners;
	protected HashMap<String, Control> controls;
	
	protected boolean mute;
	protected int level, pan;
	
	public static class InternalMetaData implements MetaDataControl
	{
		protected static ArrayList<Integer> androidMetaKeys;
		protected static HashMap<Integer, String> androidMetaToMIDP;
		
		static
		{
			androidMetaKeys = new ArrayList();
			androidMetaToMIDP = new HashMap();
			
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, TRACK_NUMBER_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_ALBUM, ALBUM_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_ARTIST, ARTIST_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_AUTHOR, AUTHOR_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_COMPOSER, COMPOSER_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_DATE, DATE_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_GENRE, GENRE_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_TITLE, TITLE_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_YEAR, YEAR_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_DURATION, DURATION_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS, NUM_TRACKS_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_WRITER, WRITER_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_MIMETYPE, MIME_TYPE_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, ALBUM_ARTIST_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER, DISC_NUMBER_KEY);
			mapMetaKey(MediaMetadataRetriever.METADATA_KEY_COMPILATION, COMPILATION_KEY);
		}
		
		protected static void mapMetaKey(int android, String midp)
		{
			androidMetaKeys.add(android);
			androidMetaToMIDP.put(android, midp);
		}
		
		protected ArrayList<String> metakeys;
		protected HashMap<String, String> metadata;
		
		public InternalMetaData()
		{
			metakeys = new ArrayList();
			metadata = new HashMap();
		}
		
		public void updateMetaData(MediaMetadataRetriever retriever)
		{
			metakeys.clear();
			metadata.clear();
			
			String key, value;
			
			for(Integer keyCode : androidMetaKeys)
			{
				value = retriever.extractMetadata(keyCode);
				
				if(value != null)
				{
					key = androidMetaToMIDP.get(keyCode);
					
					metakeys.add(key);
					metadata.put(key, value);
				}
			}
		}

		public String[] getKeys()
		{
			return metakeys.toArray(new String[0]);
		}

		public String getKeyValue(String key)
		{
			return metadata.get(key);
		}
	}
	
	public static class InternalEqualizer implements EqualizerControl
	{
		protected Equalizer equalizer;
		protected String[] presets;
		
		public InternalEqualizer(int audioSession)
		{
			equalizer = new Equalizer(0, audioSession);
		}
		
		public String[] getPresetNames()
		{
			if(presets == null)
			{
				presets = new String[equalizer.getNumberOfPresets()];
				
				for(short i = 0; i < presets.length; i++)
				{
					presets[i] = equalizer.getPresetName(i);
				}
			}
			
			return presets;
		}

		public void setPreset(String preset)
		{
			if(presets == null)
			{
				getPresetNames();
			}
			
			for(short i = 0; i < presets.length; i++)
			{
				if(presets[i].equals(preset))
				{
					equalizer.usePreset(i);
					break;
				}
			}
		}

		public String getPreset()
		{
			if(presets == null)
			{
				getPresetNames();
			}
			
			try
			{
				return presets[equalizer.getCurrentPreset()];
			}
			catch(Exception e)
			{
				return null;
			}
		}

		public void setEnabled(boolean enable)
		{
			equalizer.setEnabled(enable);
		}

		public boolean isEnabled()
		{
			return equalizer.getEnabled();
		}

		public int getNumberOfBands()
		{
			return equalizer.getNumberOfBands();
		}

		public int getBand(int frequency)
		{
			return equalizer.getBand(frequency);
		}

		public int getCenterFreq(int band)
		{
			return equalizer.getCenterFreq((short)band);
		}

		public int getMinBandLevel()
		{
			return equalizer.getBandLevelRange()[0];
		}

		public int getMaxBandLevel()
		{
			return equalizer.getBandLevelRange()[1];
		}

		public void setBandLevel(int level, int band)
		{
			equalizer.setBandLevel((short)band, (short)level);
		}

		public int getBandLevel(int band)
		{
			return equalizer.getBandLevel((short)band);
		}
	}
	
	protected InternalMetaData metadata;
	protected InternalEqualizer equalizer;
	
	public MicroPlayer()
	{
		this(null);
	}
	
	public MicroPlayer(DataSource datasource)
	{
		player = new MediaPlayer();
		
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
		
		source = datasource;
		state = UNREALIZED;
		
		mute = false;
		level = 100;
		pan = 0;
		
		metadata = new InternalMetaData();
		equalizer = new InternalEqualizer(player.getAudioSessionId());
		
		listeners = new ArrayList();
		controls = new HashMap();
		
		controls.put(VolumeControl.class.getName(), this);
		controls.put(PanControl.class.getName(), this);
		controls.put(MetaDataControl.class.getName(), metadata);
		controls.put(EqualizerControl.class.getName(), equalizer);
	}
	
	public void setDataSource(DataSource datasource) throws IOException
	{
		deallocate();
		
		if(source != null)
		{
			source.close();
		}
		
		source = datasource;
	}
	
	public MediaPlayer getMediaPlayer()
	{
		return player;
	}
	
	public Control getControl(String controlType)
	{
		if(controlType.indexOf('.') < 0)
		{
			controlType = "javax.microedition.media.control." + controlType;
		}
		
		return controls.get(controlType);
	}

	public Control[] getControls()
	{
		return controls.values().toArray(new Control[0]);
	}
	
	public void addPlayerListener(PlayerListener playerListener)
	{
		if(!listeners.contains(playerListener))
		{
			listeners.add(playerListener);
		}
	}

	public void removePlayerListener(PlayerListener playerListener)
	{
		listeners.remove(playerListener);
	}
	
	public void postEvent(String event)
	{
		for(PlayerListener listener : listeners)
		{
			listener.playerUpdate(this, event, source.getURL());
		}
	}
	
	public void onPrepared(MediaPlayer mp)
	{
		// state = PREFETCHED;
	}

	public synchronized void onCompletion(MediaPlayer mp)
	{
		postEvent(PlayerListener.END_OF_MEDIA);
		
		player.seekTo(0);
		
		if(loopCount == 1)
		{
			state = PREFETCHED;
		}
		else if(loopCount > 1)
		{
			loopCount--;
		}
		
		if(state == STARTED)
		{
			player.start();
			postEvent(PlayerListener.STARTED);
		}
	}
	
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
//		System.out.println("Error in MP " + source.getURL() + ": " + what + ", " + extra);
		return true;
	}

	public void realize() throws MediaException
	{
		checkClosed();
		
		if(source == null)
		{
			throw new IllegalStateException("call setDataSource() before calling realize()");
		}
		
		if(state == UNREALIZED)
		{
			try
			{
				MediaMetadataRetriever retriever = new MediaMetadataRetriever();
				source.setFor(retriever);
				
				metadata.updateMetaData(retriever);
				
				retriever.release();
			}
			catch(Throwable e)
			{
				source.close();
			}
			
			try
			{
				source.setFor(player);
			}
			catch(IOException e)
			{
				throw new MediaException(e);
			}
			
			state = REALIZED;
		}
	}

	public void prefetch() throws MediaException
	{
		checkClosed();
		
		if(state == UNREALIZED)
		{
			realize();
		}
		
		if(state == REALIZED)
		{
			try
			{
				player.prepare();
				state = PREFETCHED;
			}
			catch(IOException e)
			{
				throw new MediaException(e);
			}
		}
	}

	public synchronized void start() throws MediaException
	{
		prefetch();
		
		if(state == PREFETCHED)
		{
			player.start();
			
			state = STARTED;
			postEvent(PlayerListener.STARTED);
		}
	}

	public synchronized void stop()
	{
		if(state == STARTED)
		{
			player.pause();
			
			state = PREFETCHED;
			postEvent(PlayerListener.STOPPED);
		}
	}

	public void deallocate()
	{
		checkClosed();
		
		stop();
		
		if(state != UNREALIZED)
		{
			player.reset();
			state = UNREALIZED;
		}
	}

	public void close()
	{
		stop();
		
		if(state != CLOSED)
		{
			player.release();
		}
		
		source.close();
		
		state = CLOSED;
		postEvent(PlayerListener.CLOSED);
	}
	
	protected void checkClosed()
	{
		if(state == CLOSED)
		{
			throw new IllegalStateException("player is closed");
		}
	}
	
	protected void checkDataSource()
	{
		checkClosed();
		
		if(source == null)
		{
			throw new IllegalStateException("call setDataSource() before using the player");
		}
	}
	
	protected void checkRealized()
	{
		checkClosed();
		
		if(state == UNREALIZED)
		{
			throw new IllegalStateException("call realize() before using the player");
		}
	}

	public long setMediaTime(long now) throws MediaException
	{
		checkRealized();
		
		player.seekTo((int)(now / 1000L));
		return getMediaTime();
	}

	public long getMediaTime()
	{
		checkRealized();
		return player.getCurrentPosition() * 1000;
	}
	
	public long getDuration()
	{
		checkRealized();
		return player.getDuration() * 1000;
	}
	
	public void setLoopCount(int count)
	{
		checkRealized();
		
		if(count == 0)
		{
			throw new IllegalArgumentException("loop count must not be 0");
		}
		
		loopCount = count;
	}

	public int getState()
	{
		return state;
	}
	
	protected void updateVolume()
	{
		float left, right;
		
		if(mute)
		{
			left = right = 0;
		}
		else
		{
			left = right = Manager.exponentMap(level, 100);
			
			if(pan > 0)
			{
				left *= (float)(100 - pan) / 100f;
			}
			
			if(pan < 0)
			{
				right *= (float)(100 + pan) / 100f;
			}
		}
		
		player.setVolume(left, right);
		postEvent(PlayerListener.VOLUME_CHANGED);
	}

	public void setMute(boolean mute)
	{
		this.mute = mute;
		updateVolume();
	}

	public boolean isMuted()
	{
		return mute;
	}

	public int setLevel(int level)
	{
		if(level < 0)
		{
			level = 0;
		}
		else if(level > 100)
		{
			level = 100;
		}
		
		this.level = level;
		updateVolume();
		
		return level;
	}

	public int getLevel()
	{
		return level;
	}

	public int setPan(int pan)
	{
		if(pan < -100)
		{
			pan = -100;
		}
		else if(pan > 100)
		{
			pan = 100;
		}
		
		this.pan = pan;
		updateVolume();
		
		return pan;
	}

	public int getPan()
	{
		return pan;
	}

}