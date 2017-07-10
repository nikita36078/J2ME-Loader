package com.devs.acr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import ua.naiksoftware.j2meloader.R;

/**
 * @author Deven
 *         <p>
 *         Licensed under the Apache License 2.0 license see:
 *         http://www.apache.org/licenses/LICENSE-2.0
 */
public class ErrorReporterActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setFinishOnTouchOutside(false);

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppDialogTheme)
				.setMessage(R.string.crash_dialog_message)
				.setTitle(R.string.crash_dialog_title)
				.setCancelable(false)
				.setPositiveButton(R.string.report_crash, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						AutoErrorReporter.get(getApplication()).checkErrorAndSendMail(ErrorReporterActivity.this);
						System.exit(10);
					}
				})
				.setNegativeButton(R.string.CANCEL_CMD, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						System.exit(10);
					}
				});
		builder.show();
	}
}


