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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.Constants;

public class AppCenterSender implements ReportSender {
	private static final String TAG = AppCenterSender.class.getName();
	private static String BASE_URL = "https://in.appcenter.ms/logs?Api-Version=1.0.0";
	private static String FORM_KEY = "a7a26221-df9a-4e50-87a0-f76856e6e71d";

	@Override
	public void send(@NonNull Context context, @NonNull final CrashReportData report) {
		final String log = (String) report.get(AppCenterCollector.APPCENTER_LOG);
		if (log == null || log.isEmpty()) {
			return;
		}
		// Force TLSv1.2 for Android 4.1-4.4
		boolean forceTls12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;

		HurlStack hurlStack = new HurlStack(null, forceTls12 ? new TLSSocketFactory(context) : null);
		RequestQueue queue = Volley.newRequestQueue(context, hurlStack);
		StringRequest postRequest = new StringRequest(Request.Method.POST, BASE_URL,
				response -> Log.d(TAG, "send success: " + response),
				error -> {
					Log.e(TAG, "Response error", error);
					String logFile = Config.getEmulatorDir() + "/crash.txt";
					try (FileOutputStream fos = new FileOutputStream(logFile)) {
						String logcat = report.getString(ReportField.LOGCAT);
						if (logcat != null) {
							fos.write(logcat.getBytes());
						}
						String stack = report.getString(ReportField.STACK_TRACE);
						if (stack != null) {
							fos.write("\n====================Error==================\n".getBytes());
							fos.write(stack.getBytes());
						}
						JSONObject o = (JSONObject) report.get(ReportField.CUSTOM_DATA.name());
						if (o != null) {
							Object od = o.opt(Constants.KEY_APPCENTER_ATTACHMENT);
							if (od != null) {
								String customData = (String) od;
								fos.write("\n==========application=info=============\n".getBytes());
								fos.write(customData.getBytes());
							}
						}
						fos.close();
						Toast.makeText(context, "Can't send report! Saved to file:\n" + logFile, Toast.LENGTH_LONG).show();
					} catch (IOException e) {
						e.printStackTrace();
						Toast.makeText(context, "Can't send report!", Toast.LENGTH_LONG).show();
					}
				}
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
