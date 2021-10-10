package com.nokia.mid.ui;

public class SoftNotificationException extends Exception {
	private int errorCode;

	protected SoftNotificationException() {
	}

	public SoftNotificationException(String info) {
		super(info);
	}

	public SoftNotificationException(Throwable cause) {
		super(cause);
	}

	public SoftNotificationException(String info, int errorCode) {
		super(info);
		this.errorCode = errorCode;
	}

	public String toString() {
		if (this.errorCode == 0) {
			return super.toString();
		}
		return super.toString() + " Native error: " + this.errorCode;
	}

	public int getErrorCode() {
		return this.errorCode;
	}
}
