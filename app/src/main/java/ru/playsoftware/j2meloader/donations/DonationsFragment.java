/*
 *  Copyright 2022 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.playsoftware.j2meloader.donations;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import ru.playsoftware.j2meloader.R;

public class DonationsFragment extends Fragment {

	public static DonationsFragment newInstance() {
		return new DonationsFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_donations, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// Yandex
		View ymView = view.<ViewStub>findViewById(R.id.donations_ym_stub_me).inflate();

		ymView.<Button>findViewById(R.id.donations__paypal_donate_button)
				.setOnClickListener(v -> donateOnClick("https://yoomoney.ru/to/41001670387745"));

		ymView.<TextView>findViewById(R.id.donations__paypal_title)
				.setText("ЮMoney");

		// Crypto

		View btcView = view.<ViewStub>findViewById(R.id.donations_btc_stub_me).inflate();

		btcView.<Button>findViewById(R.id.donations__bitcoin_button)
				.setOnLongClickListener(v -> {
					copyOnClick("bc1qnzs6zugak4vnm08k8cvl8tkzd09rlxkw4za6gc");
					return true;
				});

		btcView.<TextView>findViewById(R.id.donations__bitcoin_title)
				.setText("BTC");

		View ethView = view.<ViewStub>findViewById(R.id.donations_eth_stub_me).inflate();

		ethView.<Button>findViewById(R.id.donations__bitcoin_button)
				.setOnLongClickListener(v -> {
					copyOnClick("0x8DF171bDB5c5a7fd0e99934554B72205797cD99E");
					return true;
				});

		ethView.<TextView>findViewById(R.id.donations__bitcoin_title)
				.setText("ETH");

		View ltcView = view.<ViewStub>findViewById(R.id.donations_ltc_stub_me).inflate();

		ltcView.<Button>findViewById(R.id.donations__bitcoin_button)
				.setOnLongClickListener(v -> {
					copyOnClick("ltc1q8r6e6lf60258vgsyw7ncjxn8pdcfg6z5w0pjjs");
					return true;
				});

		ltcView.<TextView>findViewById(R.id.donations__bitcoin_title)
				.setText("LTC");

		// PayPal

		view.<ViewStub>findViewById(R.id.donations_paypal_stub_jl).inflate()
				.<Button>findViewById(R.id.donations__paypal_donate_button)
				.setOnClickListener(v -> donateOnClick("https://www.paypal.me/j2meforever"));

		// Qiwi
		View qiwiView = view.<ViewStub>findViewById(R.id.donations_qiwi_stub_jl).inflate();

		qiwiView.<Button>findViewById(R.id.donations__paypal_donate_button)
				.setOnClickListener(v -> donateOnClick("https://my.qiwi.com/Yuryi-Khk_7vnCWvd"));

		qiwiView.<TextView>findViewById(R.id.donations__paypal_title)
				.setText("Qiwi");

	}

	public void copyOnClick(String bitcoinAddress) {
		ClipboardManager clipboard =
				(ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText(bitcoinAddress, bitcoinAddress);
		clipboard.setPrimaryClip(clip);
		Toast.makeText(getActivity(), org.sufficientlysecure.donations.R.string.donations__bitcoin_toast_copy,
				Toast.LENGTH_SHORT).show();
	}

	public void donateOnClick(String uri) {
		Uri payPalUri = Uri.parse(uri);
		Intent intent = new Intent(Intent.ACTION_VIEW, payPalUri);

		try {
			startActivity(intent);
			requireActivity().finish();
		} catch (Exception e) {
			e.printStackTrace();
			openDialog(org.sufficientlysecure.donations.R.string.donations__alert_dialog_title,
					getString(org.sufficientlysecure.donations.R.string.donations__alert_dialog_no_browser));
		}
	}

	void openDialog(int title, String message) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(requireActivity());
		dialog.setIcon(android.R.drawable.ic_dialog_alert);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setCancelable(true);
		dialog.setPositiveButton(org.sufficientlysecure.donations.R.string.donations__button_close, null);
		dialog.show();
	}
}
