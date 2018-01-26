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

package ua.naiksoftware.j2meloader;

import android.content.Context;
import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HockeySender implements ReportSender {
	private static String BASE_URL = "https://rink.hockeyapp.net/api/2/apps/";
	private static String FORM_KEY = "27884e4164834206ae60c8cf4c367720";
	private static String CRASHES_PATH = "/crashes";

	@Override
	public void send(Context context, CrashReportData report) {
		String log = createCrashLog(report);
		String url = BASE_URL + FORM_KEY + CRASHES_PATH;
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);

			List<NameValuePair> parameters = new ArrayList<>();
			parameters.add(new BasicNameValuePair("raw", log));
			httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));

			httpClient.execute(httpPost);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String createCrashLog(CrashReportData report) {
		Date now = new Date();
		String log = ("Package: " + report.getString(ReportField.PACKAGE_NAME) + "\n") +
				"Version name: " + report.getString(ReportField.APP_VERSION_NAME) + "\n" +
				"Version: " + report.getString(ReportField.APP_VERSION_CODE) + "\n" +
				"Android: " + report.getString(ReportField.ANDROID_VERSION) + "\n" +
				"Manufacturer: " + report.getString(ReportField.BRAND) + "\n" +
				"Model: " + report.getString(ReportField.PHONE_MODEL) + "\n" +
				"App info: " + report.getString(ReportField.CUSTOM_DATA) + "\n" +
				"Date: " + now + "\n" + "\n" + report.getString(ReportField.STACK_TRACE);
		return log;
	}
}
