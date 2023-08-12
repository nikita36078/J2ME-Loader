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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		int id = intent.getIntExtra("id", 0);
		int event = intent.getIntExtra("event", 0);
		SoftNotificationImpl inst = SoftNotificationImpl.instanceMap.get(id);
		if (inst != null) {
			inst.notificationCallback(event);
			try {
				if (event == SoftNotificationImpl.EVENT_DISMISS) {
					inst.remove();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
