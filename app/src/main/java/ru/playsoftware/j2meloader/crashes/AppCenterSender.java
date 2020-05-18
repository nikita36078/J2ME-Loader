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

package ru.playsoftware.j2meloader.crashes;

import android.content.Context;
import android.os.Build;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;

public class AppCenterSender implements ReportSender {
	private static final String TAG = AppCenterSender.class.getName();
	private static String BASE_URL = "https://in.appcenter.ms/logs?Api-Version=1.0.0";
	private static String FORM_KEY = "a7a26221-df9a-4e50-87a0-f76856e6e71d";

	@Override
	public void send(@NonNull Context context, @NonNull final CrashReportData report) {
		final String log = (String) report.get(AppCenterCollector.APPCENTER_LOG);

		HurlStack hurlStack = new HurlStack() {
			@Override
			protected HttpURLConnection createConnection(URL url) throws IOException {
				HttpsURLConnection httpsURLConnection = (HttpsURLConnection) super.createConnection(url);
				try {
					// Force TLSv1.2 for Android 4.1-4.4
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
							&& Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
						httpsURLConnection.setSSLSocketFactory(new TLSSocketFactory());
					}
				} catch (KeyManagementException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				return httpsURLConnection;
			}
		};
		RequestQueue queue = Volley.newRequestQueue(context, hurlStack);
		StringRequest postRequest = new StringRequest(Request.Method.POST, BASE_URL, null,
				error -> android.util.Log.e(TAG, "Response error")
		) {
			@Override
			public Map<String, String> getHeaders() {
				Map<String, String> params = new HashMap<>();
				params.put("Content-Type", "application/json");
				params.put("App-Secret", FORM_KEY);
				params.put("Install-ID", report.getString(ReportField.INSTALLATION_ID));
				return params;
			}

			@Override
			public byte[] getBody() {
				return log.getBytes();
			}
		};
		postRequest.setShouldCache(false);
		queue.add(postRequest);
	}
}
