package com.nokia.mid.ui;

public interface SoftNotificationListener {
	void notificationSelected(SoftNotification notification);

	void notificationDismissed(SoftNotification notification);
}
