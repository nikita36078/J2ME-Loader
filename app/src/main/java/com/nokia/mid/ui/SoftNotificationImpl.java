package com.nokia.mid.ui;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;

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

	private SoftNotificationListener[] iListener;
	private String groupText;
	private String text;
	private boolean hasImage;
	private static SoftNotificationImpl lastInstance;
	private Notification notification;
	private String softAction1;
	private String softAction2;
	private int id;
	private static int ids = 0;
	private static Hashtable<Integer, SoftNotificationImpl> notifications;
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
			old = notifications.get(id);
			notification = old.notification;
		}
	}

	private void notificationCallback(int aEventArg) {
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
		System.out.println("SoftNotificationImpl post");
		try {
			if(id == -1) id = ids++;
			notifications.put(id, this);
			PendingIntent pendingIntent = null;
			if(softAction1 != null || softAction2 != null) {
				Intent intent = new Intent(context, this.getClass());
				intent.putExtra(EXTRA_NOTIFICATION_ID, id);
				pendingIntent = PendingIntent.getActivity(context, id, intent, 0);
			}
			String appName = context.getAppName();
			String channelId = appName.toLowerCase();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
				NotificationChannel channel = null;
				for(NotificationChannel c: notificationManager.getNotificationChannels()) {
					if(c.getId().equals(channelId)) {
						channel = c;
						break;
					}
				}
				if(channel == null) {
					int importance = NotificationManager.IMPORTANCE_DEFAULT;
					channel = new NotificationChannel(channelId, appName, importance);
					channel.setDescription("MIDlet");
					notificationManager.createNotificationChannel(channel);
				}
			}
			NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
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
			if(pendingIntent != null) builder.setContentIntent(pendingIntent);
			if(softAction1 != null) {
				NotificationCompat.Action action = new NotificationCompat.Action.Builder(null, softAction1, pendingIntent).build();
				builder.addAction(action);
			}
			if(softAction2 != null) {
				NotificationCompat.Action action = new NotificationCompat.Action.Builder(null, softAction2, pendingIntent).build();
				builder.addAction(action);
			}
			notification = builder.build();
			notificationmgr.notify(id, notification);
			System.out.println("SoftNotificationImpl post success");
		} catch (Throwable e) {
			System.out.println("SoftNotificationImpl ERROR: " + e.toString());
			e.printStackTrace();
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
		if(notification != null) {
			try {
				post();
			} catch (Exception e) {
				throw new SoftNotificationException(e.toString());
			}
		}
	}

	public void setSoftkeyLabels(String aSoftkey1Label, String aSoftkey2Label) throws SoftNotificationException {
		softAction1 = aSoftkey1Label;
		softAction2 = aSoftkey2Label;
		if(notification != null) {
			try {
				post();
			} catch (Exception e) {
				throw new SoftNotificationException(e.toString());
			}
		}
	}

	public void setImage(byte[] aImageData) throws SoftNotificationException {
		Bitmap b = PNGUtils.getFixedBitmap(aImageData, 0, aImageData.length);
		if (b == null) {
			throw new SoftNotificationException("Can't decode image");
		}
		bitmap = b;
		if(notification != null) {
			try {
				post();
			} catch (Exception e) {
				throw new SoftNotificationException(e.toString());
			}
		}
		hasImage = true;
	}

	private static NotificationManagerCompat notificationmgr;
	private static MicroActivity context;

	static {
		try {
			context = ContextHolder.getActivity();
			notificationmgr = NotificationManagerCompat.from(context);
			notifications = new Hashtable<Integer, SoftNotificationImpl>();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected static void action(int i) {
		if(lastInstance != null) {
			lastInstance.notificationCallback(i);
		}
	}
}
