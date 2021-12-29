package com.nokia.mid.ui;

public abstract class SoftNotification {
	public static SoftNotification newInstance(int notificationId) {
		return new SoftNotificationImpl(notificationId);
	}

	public static SoftNotification newInstance() {
		return new SoftNotificationImpl();
	}

	public abstract int getId();

	public abstract void post() throws SoftNotificationException;

	public abstract void remove() throws SoftNotificationException;

	public abstract void setListener(SoftNotificationListener listener);

	public abstract void setText(String text, String groupText) throws SoftNotificationException;

	public abstract void setSoftkeyLabels(String softkey1Label, String softkey2Label) throws SoftNotificationException;

	public abstract void setImage(byte[] image) throws SoftNotificationException;
}
