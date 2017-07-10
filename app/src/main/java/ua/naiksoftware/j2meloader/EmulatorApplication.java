package ua.naiksoftware.j2meloader;

import android.app.Application;

import com.devs.acr.AutoErrorReporter;

public class EmulatorApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		AutoErrorReporter.get(this)
				.setEmailAddresses("j2me.loader@mail.ru")
				.setEmailSubject("Auto Crash Report")
				.start();
	}
}
