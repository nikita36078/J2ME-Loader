package com.nokia.mid.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import javax.microedition.util.ContextHolder;

public class NotificationActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		int id = intent.getIntExtra("id", 0);
		int event = intent.getIntExtra("event", 0);
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
		this.finish();
	}
}