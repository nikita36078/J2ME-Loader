package ua.naiksoftware.j2meloader;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutDialogFragment extends DialogFragment {
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		StringBuilder message = new StringBuilder().append(getText(R.string.about_message))
				.append(getText(R.string.version))
				.append(BuildConfig.VERSION_NAME)
				.append(getText(R.string.about_email))
				.append(getText(R.string.about_github))
				.append(getText(R.string.about_4pda));
		TextView tv = new TextView(getActivity());
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(Html.fromHtml(message.toString()));
		tv.setTextSize(16);
		tv.setPadding(10, 0, 0, 0);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon_java)
				.setView(tv);
		return builder.create();
	}
}
