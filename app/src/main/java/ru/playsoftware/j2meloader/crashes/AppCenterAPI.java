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

import java.util.ArrayList;

public class AppCenterAPI {
	static class RequestData {
		public ArrayList<Log> logs;
	}

	static class Log {
		public String id;
		public Device device;
		public String timestamp;
	}

	static class ErrorLog extends Log {
		public boolean fatal = true;
		public Exception exception;
		public String appLaunchTimestamp;
		public int processId = 0;
		public String processName = "";
		public String errorThreadName;
		public String type = "managedError";
	}

	static class Device {
		public String appNamespace;
		public String appVersion;
		public String appBuild;
		public String sdkName = "appcenter.android";
		public String sdkVersion = "1.0.0";
		public String osName = "Android";
		public String osVersion;
		public String oemName;
		public String model;
		public String locale;
	}

	static class Exception {
		public String type;
		public String message;
		public ArrayList<ExceptionFrame> frames;
		public String stackTrace;
		public ArrayList<Exception> innerExceptions;
	}

	static class ExceptionFrame {
		public String className;
		public String methodName;
		public String fileName;
		public int lineNumber;
	}

	static class ErrorAttachmentLog extends Log {
		public String errorId;
		public String contentType = "text/plain";
		public String data;
		public String type = "errorAttachment";
	}
}
