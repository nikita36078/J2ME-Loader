package com.nokia.mid.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
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

	private SoftNotificationListener[] iListener;
	private String groupText;
	private String text;
	private boolean hasImage;
	private static SoftNotificationImpl lastInstance;
	private Notification notification;
	private String softAction1;
	private String softAction2;
	private int id;
	private static int ids = 1;
	static Hashtable<Integer, SoftNotificationImpl> instanceMap;
	private SoftNotificationImpl old;
	private Bitmap bitmap;

	public SoftNotificationImpl(int aNotificationId) {
		initialize(aNotificationId);
	}

	public SoftNotificationImpl() {
		initialize(-1);
	}

	protected void initialize(int aNotificationId) {
		id = aNotificationId;
		lastInstance = this;
		iListener = new SoftNotificationListener[1];
		if(id != -1) {
			old = instanceMap.get(id);
			notification = old.notification;
		}
	}

	void notificationCallback(int aEventArg) {
		synchronized (this.iListener) {
			SoftNotificationListener listener = this.iListener[0];
			if (listener != null) {
				if (aEventArg == 1) {
					listener.notificationSelected(this);
				} else if (aEventArg == 2) {
					listener.notificationDismissed(this);
				}
			}
		}
	}

	public int getId() {
		if(notification == null) return -1;
		return id;
	}

	public void post() throws SoftNotificationException {
		try {
			if(id == -1) id = ids++;
			instanceMap.put(id, this);

			String appName = activity.getAppName();
			String channelId = appName.toLowerCase();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = null;
				for(NotificationChannel c: notificationmgr.getNotificationChannels()) {
					if(c.getId().equals(channelId)) {
						channel = c;
						break;
					}
				}
				if(channel == null) {
					int importance = NotificationManager.IMPORTANCE_DEFAULT;
					channel = new NotificationChannel(channelId, appName, importance);
					channel.setDescription("MIDlet");
					notificationmgr.createNotificationChannel(channel);
				}
			}
			NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, channelId);
			builder.setContentTitle(appName);
			if(groupText != null) builder.setGroup(groupText);
			if(text != null) builder.setContentText(text);
			if(bitmap != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					builder.setSmallIcon(IconCompat.createWithBitmap(bitmap));
				} else {
					builder.setSmallIcon(R.mipmap.ic_launcher);
				}
			} else {
				builder.setSmallIcon(R.mipmap.ic_launcher);
			}
			builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

			Intent selectIntent = new Intent(activity, NotificationActivity.class);
			selectIntent.setAction("select");
			selectIntent.putExtra("id", id);
			selectIntent.putExtra("event", 1);
			PendingIntent selectPendingIntent = PendingIntent.getActivity(activity, (int) System.currentTimeMillis(), selectIntent, 0);
			builder.setContentIntent(selectPendingIntent);
			if(softAction1 != null) {
				builder.addAction(new NotificationCompat.Action.Builder(null,
						softAction1, selectPendingIntent)
						.build());
			}

			Intent dismissIntent = new Intent(activity, NotificationActivity.class);
			dismissIntent.setAction("dismiss");
			dismissIntent.putExtra("id", id);
			dismissIntent.putExtra("event", 2);

			NotificationCompat.Action dismissAction =
					new NotificationCompat.Action.Builder(null,
							softAction2 != null ? softAction2 : "Dismiss",
							PendingIntent.getActivity(activity, (int) System.currentTimeMillis(), dismissIntent, 0))
							.build();

			builder.addAction(dismissAction);
			notification = builder.build();
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notificationmgr.notify(id, notification);
		} catch (Throwable e) {
			throw new SoftNotificationException(e);
		}
	}

	public void remove() throws SoftNotificationException {
		if(notification == null) throw new SoftNotificationException("not posted");
		notificationmgr.cancel(id);
	}

	public void setListener(SoftNotificationListener aListener) {
		synchronized (iListener) {
			iListener[0] = aListener;
		}
	}

	public void setText(String aText, String aGroupText) throws SoftNotificationException {
		text = aText;
		groupText = aGroupText;
	}

	public void setSoftkeyLabels(String aSoftkey1Label, String aSoftkey2Label) throws SoftNotificationException {
		softAction1 = aSoftkey1Label;
		softAction2 = aSoftkey2Label;
	}

	public void setImage(byte[] aImageData) throws SoftNotificationException {
		Bitmap b = PNGUtils.getFixedBitmap(aImageData, 0, aImageData.length);
		if (b == null) {
			throw new SoftNotificationException("Can't decode image");
		}
		bitmap = b;
		hasImage = true;
	}

	private static NotificationManagerCompat notificationmgr;
	private static MicroActivity activity;

	static {
		try {
			activity = ContextHolder.getActivity();
			notificationmgr = NotificationManagerCompat.from(activity);
			instanceMap = new Hashtable<Integer, SoftNotificationImpl>();
		} catch (Exception ex) {
		}
	}
}
