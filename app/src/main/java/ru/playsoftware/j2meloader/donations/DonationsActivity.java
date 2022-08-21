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

package ru.playsoftware.j2meloader.donations;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.sufficientlysecure.donations.DonationsFragment;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import ru.playsoftware.j2meloader.BuildConfig;
import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.base.BaseActivity;

public class DonationsActivity extends BaseActivity {

	/**
	 * Google
	 */
	private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjH1jO9oXfAiubICi+mOr2uJuIfqZI6hVgWPmJnOc08F6+Qe0c9sLEISiXXQXrh9rvGfc6jloN/QlsA5yXEBM3V/SVX48KpPb46mAUC+jLQCURcAazSYG7Orc6+Zo1y3JUorTOem0lSMd46uRyrRwdcsw4IPjHZV9GHfCFxz6Jm5hl3r7n5SHFZBJhixDGpoL4/HxSgtbT/kdtg7EsStB5NtnBsNzGZj+JNAyZ5yxL/7aRLQtNFLOPMPNhzMblYekcmr1IzH0MxxlpnwOOS7712InwmpRCoiAdP8wVsohi1f7DtHh0sYGYSoEwqkwG6lHrgx+XkIOm8U0l9YJphJcGQIDAQAB";

	private static final String[] GOOGLE_CATALOG = new String[]{"j2me.donation.1",
			"j2me.donation.2", "j2me.donation.3", "j2me.donation.4"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donations);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.donate);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment donationsFragment;
		if (BuildConfig.DONATIONS_GOOGLE) {
			donationsFragment = DonationsFragment.newInstance(BuildConfig.DEBUG, true, GOOGLE_PUBKEY, GOOGLE_CATALOG,
					getResources().getStringArray(R.array.donation_google_catalog_values), false, null, null,
					null, false, null, null, false, null);
		} else {
			donationsFragment = ru.playsoftware.j2meloader.donations.DonationsFragment.newInstance();
		}

		ft.replace(R.id.donations_activity_container, donationsFragment, "donationsFragment");
		ft.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag("donationsFragment");
		if (fragment != null) {
			fragment.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

}