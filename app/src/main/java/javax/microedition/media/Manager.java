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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.microedition.io.file.FileConnection;
import javax.microedition.util.ContextHolder;

import android.webkit.MimeTypeMap;

public class Manager
{
	private static class StreamCacheCleaner implements PlayerListener
	{
		public void playerUpdate(Player player, String event, Object eventData)
		{
			if(PlayerListener.CLOSED.equals(event) && eventData instanceof String)
			{
				event = (String)eventData;
				int index = event.lastIndexOf('/');
				
				if(index >= 0)
				{
					event = event.substring(index + 1);
				}
				
				File file = new File(ContextHolder.getCacheDir(), event);
				
				if(file.delete())
				{
					System.out.println("Temp file deleted: " + event);
				}
			}
		}
	}
	
	private static StreamCacheCleaner cleaner = new StreamCacheCleaner();
	
	public static Player createPlayer(String locator) throws IOException
	{
		return new MicroPlayer(new DataSource(locator));
	}
	
	public static Player createPlayer(final InputStream stream, String type) throws IOException
	{
		if(type != null)
		{
			type = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
			
			if(type != null)
			{
				type = "." + type;
			}
		}
		
		File file = File.createTempFile("media", type, ContextHolder.getCacheDir());
		final RandomAccessFile raf = new RandomAccessFile(file, "rw");
		
		final String name = file.getName();
		System.out.println("Starting media pipe: " + name);
		
		int length = stream.available();
		
		if(length >= 0)
		{
			raf.setLength(length);
			System.out.println("Changing file size to " + length + " bytes: " + name);
		}
		
		final Object sync = new Object();
		
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				byte[] buf = new byte[0x10000];
				int read;
				
				try
				{
					while(true)
					{
						read = stream.read(buf);
						
						if(read > 0)
						{
							synchronized(sync)
							{
								raf.write(buf, 0, read);
							}
						}
						else if(read < 0)
						{
							break;
						}
					}
					
					raf.close();
					
					System.out.println("Media pipe closed: " + name);
				}
				catch(IOException e)
				{
					System.out.println("Media pipe failure: " + e.toString());
				}
			}
		};
		
		Thread thread = new Thread(runnable);
		thread.start();
		
		try
		{
			MicroPlayer player = new MicroPlayer();
			player.addPlayerListener(cleaner);
			
			DataSource source = new DataSource(file);
			
			try
			{
				player.setDataSource(source);
			}
			catch(IOException e)
			{
				source.close();
				
				if(thread.isAlive())
				{
					System.out.println("Waiting for pipe to close: " + name);
					try
					{
						thread.join();
					}
					catch(InterruptedException ie)
					{
					}
					
					player.setDataSource(source);
				}
				else
				{
					throw e;
				}
			}
			
			return player;
		}
		catch(IOException e)
		{
			try
			{
				synchronized(sync)
				{
					raf.close();
				}
			}
			catch(IOException x)
			{
				System.out.println("File is not closing: " + name);
			}
			
			cleaner.playerUpdate(null, PlayerListener.CLOSED, name);
			
			throw e;
		}
	}
	
	public static float exponentMap(float value, float range)
	{
		if(value <= 0)
		{
			return 0;
		}
		else if(value >= range)
		{
			return 1;
		}
		else
		{
			return (float)Math.pow(10, 72 * (value - range) / 20 / range);
		}
	}
	
	public static String[] getSupportedContentTypes(String str) {
        return new String[]{"audio/*", "video/*", "audio/wav", "audio/x-tone-seq", "audio/x-wav", "audio/midi", "audio/x-midi", "audio/mpeg", "audio/amr", "audio/amr-wb", "audio/mp3", "audio/mp4", "video/mpeg", "video/mp4", "video/mpeg4", "video/3gpp"};
	}
	
	public synchronized static void playTone(int frequency, int time, int volume)
	throws MediaException 
	{
		//TODO method stub
	}
}
