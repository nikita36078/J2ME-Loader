/*
 * Copyright 2020 Nikita Shakarun
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
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;

import org.acra.ReportField;
import org.acra.builder.ReportBuilder;
import org.acra.collector.Collector;
import org.acra.config.CoreConfiguration;
import org.acra.data.CrashReportData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@AutoService(Collector.class)
public class AppCenterCollector implements Collector {
	public static final String APPCENTER_LOG = "APPCENTER_LOG";

	@Override
	public void collect(@NonNull Context context,
						@NonNull CoreConfiguration config,
						@NonNull ReportBuilder reportBuilder,
						@NonNull CrashReportData crashReportData) {
		String log = createCrashLog(crashReportData, reportBuilder.getException());
		crashReportData.put(APPCENTER_LOG, log);
	}

	@NonNull
	@Override
	public Order getOrder() {
		return Order.LAST;
	}

	private String createCrashLog(CrashReportData report, Throwable t) {
		AppCenterAPI.ErrorLog errorLog = new AppCenterAPI.ErrorLog();
		errorLog.appLaunchTimestamp = report.getString(ReportField.USER_APP_START_DATE);
		errorLog.id = report.getString(ReportField.REPORT_ID);
		errorLog.timestamp = report.getString(ReportField.USER_CRASH_DATE);
		errorLog.errorThreadName = report.getString(ReportField.THREAD_DETAILS);

		AppCenterAPI.Device device = new AppCenterAPI.Device();
		String versionName = report.getString(ReportField.APP_VERSION_NAME);
		if (versionName != null) {
			int id = versionName.indexOf('-');
			if (id > 0) {
				versionName = versionName.substring(0, id);
			}
		}
		device.appBuild = report.getString(ReportField.APP_VERSION_CODE);
		device.appNamespace = report.getString(ReportField.PACKAGE_NAME);
		device.appVersion = versionName;
		device.model = report.getString(ReportField.PHONE_MODEL);
		device.osVersion = report.getString(ReportField.ANDROID_VERSION);
		device.oemName = report.getString(ReportField.BRAND);
		device.locale = Locale.getDefault().toString();
		errorLog.device = device;

		errorLog.exception = getModelExceptionFromThrowable(t);

		AppCenterAPI.ErrorAttachmentLog errorAttachmentLog =
				new AppCenterAPI.ErrorAttachmentLog();
		String customData = report.getString(ReportField.CUSTOM_DATA);
		if (customData != null) {
			byte[] attachment = customData.getBytes();
			errorAttachmentLog.data = Base64.encodeToString(attachment, Base64.DEFAULT);
		}
		errorAttachmentLog.errorId = report.getString(ReportField.REPORT_ID);
		errorAttachmentLog.device = device;
		errorAttachmentLog.timestamp = report.getString(ReportField.USER_CRASH_DATE);

		AppCenterAPI.RequestData requestData = new AppCenterAPI.RequestData();
		ArrayList<AppCenterAPI.Log> logs = new ArrayList<>();
		logs.add(errorLog);
		logs.add(errorAttachmentLog);
		requestData.logs = logs;
		Gson gson = new Gson();
		return gson.toJson(requestData);
	}

	private AppCenterAPI.Exception getModelExceptionFromThrowable(Throwable t) {
		AppCenterAPI.Exception topException = null;
		AppCenterAPI.Exception parentException = null;
		List<Throwable> causeChain = new LinkedList<>();
		for (Throwable cause = t; cause != null; cause = cause.getCause()) {
			causeChain.add(cause);
		}
		for (Throwable cause : causeChain) {
			AppCenterAPI.Exception exception = new AppCenterAPI.Exception();
			exception.type = cause.getClass().getName();
			exception.message = cause.getMessage();
			exception.frames = getModelFramesFromStackTrace(cause.getStackTrace());
			exception.stackTrace = getStackTraceString(cause.getStackTrace());
			if (topException == null) {
				topException = exception;
			} else {
				ArrayList<AppCenterAPI.Exception> list = new ArrayList<>();
				list.add(exception);
				parentException.innerExceptions = list;
			}
			parentException = exception;
		}
		return topException;
	}

	private String getStackTraceString(StackTraceElement[] stackTrace) {
		StringBuilder trace = new StringBuilder();
		for (StackTraceElement stackTraceElement : stackTrace) {
			trace.append("\tat ").append(stackTraceElement).append("\n");
		}
		return trace.toString();
	}

	private ArrayList<AppCenterAPI.ExceptionFrame> getModelFramesFromStackTrace(StackTraceElement[] stackTrace) {
		ArrayList<AppCenterAPI.ExceptionFrame> stackFrames = new ArrayList<>();
		for (StackTraceElement stackTraceElement : stackTrace) {
			stackFrames.add(getModelStackFrame(stackTraceElement));
		}
		return stackFrames;
	}

	private AppCenterAPI.ExceptionFrame getModelStackFrame(StackTraceElement stackTraceElement) {
		AppCenterAPI.ExceptionFrame stackFrame = new AppCenterAPI.ExceptionFrame();
		stackFrame.className = stackTraceElement.getClassName();
		stackFrame.methodName = stackTraceElement.getMethodName();
		stackFrame.lineNumber = stackTraceElement.getLineNumber();
		stackFrame.fileName = stackTraceElement.getFileName();
		return stackFrame;
	}
}
