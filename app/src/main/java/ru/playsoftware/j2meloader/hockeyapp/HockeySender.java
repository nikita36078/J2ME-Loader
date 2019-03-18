/*
 * Copyright 2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader.hockeyapp;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class HockeySender implements ReportSender {
	private static final String TAG = HockeySender.class.getName();
	private static String BASE_URL = "https://rink.hockeyapp.net/api/2/apps/";
	private static String FORM_KEY = "89da3b5b92414df1833110eba7d26332";
	private static String CRASHES_PATH = "/crashes";

	@Override
	public void send(@NonNull Context context, @NonNull final CrashReportData report) {
		final String log = createCrashLog(report);
		String url = BASE_URL + FORM_KEY + CRASHES_PATH;

		RequestQueue queue = Volley.newRequestQueue(context);
		StringRequest postRequest = new StringRequest(Request.Method.POST, url, null,
				error -> Log.e(TAG, "Response error")
		) {
			@Override
			protected Map<String, String> getParams() {
				Map<String, String> params = new HashMap<>();
				params.put("raw", log);
				params.put("userID", report.getString(ReportField.INSTALLATION_ID));
				return params;
			}
		};
		postRequest.setShouldCache(false);
		queue.add(postRequest);
	}

	private String createCrashLog(CrashReportData report) {
		Date now = new Date();
		return ("Package: " + report.getString(ReportField.PACKAGE_NAME) + "\n") +
				"Version name: " + report.getString(ReportField.APP_VERSION_NAME) + "\n" +
				"Version: " + report.getString(ReportField.APP_VERSION_CODE) + "\n" +
				"Android: " + report.getString(ReportField.ANDROID_VERSION) + "\n" +
				"Manufacturer: " + report.getString(ReportField.BRAND) + "\n" +
				"Model: " + report.getString(ReportField.PHONE_MODEL) + "\n" +
				"App info: " + report.getString(ReportField.CUSTOM_DATA) + "\n" +
				"Date: " + now + "\n" + "\n" + report.getString(ReportField.STACK_TRACE);
	}
}
