package com.nokia.mid.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("NotifActivity onCreate 1");
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		int id = intent.getIntExtra("id", 0);
		int event = intent.getIntExtra("event", 0);
		System.out.println("NotifActivity onCreate " + id + " " + event);
		SoftNotificationImpl inst = SoftNotificationImpl.instanceMap.get(id);
		if (inst != null) {
			inst.notificationCallback(event);
			try {
				if (event == 2) {
					inst.remove();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}