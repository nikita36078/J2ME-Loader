/*
 * Copyright 2017-2018 Nikita Shakarun
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

package ru.playsoftware.j2meloader;

import android.app.Application;
import android.content.Context;
import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;

@AcraCore(reportSenderFactoryClasses = {HockeySenderFactory.class})
@AcraDialog(resTitle = R.string.crash_dialog_title, resText = R.string.crash_dialog_message,
		resPositiveButtonText = R.string.report_crash, resNegativeButtonText = R.string.CANCEL_CMD, resTheme = R.style.AppTheme_Dialog)
public class EmulatorApplication extends Application {
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		ACRA.init(this);
	}
}
