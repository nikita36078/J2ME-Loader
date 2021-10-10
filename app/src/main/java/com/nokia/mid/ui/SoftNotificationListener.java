package com.nokia.mid.ui;

public abstract interface SoftNotificationListener {
	public abstract void notificationSelected(SoftNotification paramSoftNotification);

	public abstract void notificationDismissed(SoftNotification paramSoftNotification);
}
