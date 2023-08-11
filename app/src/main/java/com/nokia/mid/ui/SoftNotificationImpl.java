/*
 * Copyright 2021 Arman Jussupgaliyev
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

package com.nokia.mid.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Hashtable;

import javax.microedition.shell.MicroActivity;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.util.PNGUtils;

public class SoftNotificationImpl extends SoftNotification {
	final static int EVENT_ACCEPT = 1;
	final static int EVENT_DISMISS = 2;

	@SuppressLint("StaticFieldLeak")
	private static NotificationManagerCompat notificationmgr;
	@SuppressLint("StaticFieldLeak")
	private static MicroActivity activity;

	private SoftNotificationListener[] listeners;
	private String groupText;
	private String text;
	private Notification notification;
	private String softAction1;
	private String softAction2;
	private int id;
	private static int ids = 1;
	static Hashtable<Integer, SoftNotificationImpl> instanceMap;
	private SoftNotificationImpl old;
	private Bitmap bitmap;

	static {
		try {
			activity = ContextHolder.getActivity();
			notificationmgr = NotificationManagerCompat.from(activity);
			instanceMap = new Hashtable<>();
		} catch (Exception ignored) {
		}
	}

	public SoftNotificationImpl(int notificationId) {
		initialize(notificationId);
	}

	public SoftNotificationImpl() {
		initialize(-1);
	}

	protected void initialize(int notificationId) {
		id = notificationId;
		listeners = new SoftNotificationListener[1];
		if (id != -1) {
			old = instanceMap.get(id);
			notification = old.notification;
		}
	}

	void notificationCallback(int eventArg) {
		synchronized (this.listeners) {
			SoftNotificationListener listener = this.listeners[0];
			if (listener != null) {
				if (eventArg == EVENT_ACCEPT) {
					listener.notificationSelected(this);
				} else if (eventArg == EVENT_DISMISS) {
					listener.notificationDismissed(this);
				}
			}
		}
	}

	public int getId() {
		if (notification == null) return -1;
		return id;
	}

	public void post() throws SoftNotificationException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
				!ContextHolder.requestPermission(Manifest.permission.POST_NOTIFICATIONS)) {
			throw new SoftNotificationException();
		}
		try {
			if (id == -1) id = ids++;
			instanceMap.put(id, this);
			String appName = activity.getAppName();
			String channelId = appName.toLowerCase();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = notificationmgr.getNotificationChannel(channelId);
				if (channel == null) {
					int importance = NotificationManager.IMPORTANCE_DEFAULT;
					channel = new NotificationChannel(channelId, appName, importance);
					channel.setDescription("MIDlet");
					notificationmgr.createNotificationChannel(channel);
				}
			}
			NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, channelId);
			builder.setContentTitle(appName);
			if (text != null) builder.setContentText(text);
			if (groupText != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				builder.setGroup(groupText);
			}
			if (bitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				builder.setSmallIcon(IconCompat.createWithBitmap(bitmap));
			} else {
				builder.setSmallIcon(R.mipmap.ic_launcher);
			}
			builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
			builder.setAutoCancel(true);

			@SuppressLint("InlinedApi")
			int pendingIntentFlags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : 0;

			Intent selectIntent = new Intent(activity, NotificationBroadcastReceiver.class);
			selectIntent.putExtra("id", id);
			selectIntent.putExtra("event", EVENT_ACCEPT);

			PendingIntent selectPendingIntent = PendingIntent.getBroadcast(activity,
					(int) System.currentTimeMillis(), selectIntent, pendingIntentFlags);
			builder.setContentIntent(selectPendingIntent);
			builder.addAction(new NotificationCompat.Action.Builder(null,
					softAction1 != null ? softAction2 : activity.getString(R.string.show),
					selectPendingIntent)
					.build());

			Intent dismissIntent = new Intent(activity, NotificationBroadcastReceiver.class);
			dismissIntent.putExtra("id", id);
			dismissIntent.putExtra("event", EVENT_DISMISS);

			PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(activity,
					(int) System.currentTimeMillis(), dismissIntent, pendingIntentFlags);
			builder.addAction(new NotificationCompat.Action.Builder(null,
					softAction2 != null ? softAction2 : activity.getString(R.string.dismiss),
					dismissPendingIntent)
					.build());

			notification = builder.build();
			notificationmgr.notify(id, notification);
		} catch (Throwable e) {
			throw new SoftNotificationException(e);
		}
	}

	public void remove() throws SoftNotificationException {
		if (notification == null) throw new SoftNotificationException("not posted");
		notificationmgr.cancel(id);
	}

	public void setListener(SoftNotificationListener listener) {
		synchronized (listeners) {
			listeners[0] = listener;
		}
	}

	public void setText(String text, String groupText) throws SoftNotificationException {
		this.text = text;
		this.groupText = groupText;
	}

	public void setSoftkeyLabels(String softkey1Label, String softkey2Label) throws SoftNotificationException {
		softAction1 = softkey1Label;
		softAction2 = softkey2Label;
	}

	public void setImage(byte[] imageData) throws SoftNotificationException {
		Bitmap b = PNGUtils.getFixedBitmap(imageData, 0, imageData.length);
		if (b == null) {
			throw new SoftNotificationException("Can't decode image");
		}
		bitmap = b;
	}
}
