package org.simonallen.pingthing;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class NewServerActivity extends AppCompatActivity {
	private EditText mName;
	private EditText mHost;
	private CheckBox mICMP;
	private EditText mPort;
	private TextView mStatus;
	private TextView mTime;
	private ProgressBar mPingProgressBar;
	private LinearLayout mTest;
	NumberFormat mMsFormatter = new DecimalFormat("#0.00");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_new_server);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_new);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		mName = (EditText) findViewById(R.id.exittext_name);
		mHost = (EditText) findViewById(R.id.edittext_host);
		mICMP = (CheckBox) findViewById(R.id.icmp);
		mPort = (EditText) findViewById(R.id.port);
		mStatus = (TextView) findViewById(R.id.status);
		mTime = (TextView) findViewById(R.id.time);
		mPingProgressBar = (ProgressBar) findViewById(R.id.progressBar_ping);
		mTest = (LinearLayout) findViewById(R.id.linearLayout_test);

		mName.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				mTest.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		mICMP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mPort.setEnabled(false);
					mPort.setText("");

					ViewGroup rootView = (ViewGroup) mPort.getRootView();
					int df = rootView.getDescendantFocusability();
					rootView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
					mPort.clearFocus();
					rootView.setDescendantFocusability(df);
				} else {
					mPort.setEnabled(true);
					mPort.requestFocus();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.toolbar_new, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent();

		switch (item.getItemId()) {
			case R.id.test:
				if (!validateAll())
					break;

				new Thread() {
					private MenuItem mItem;

					@Override
					public void run() {
						pingTest(mItem);
					}

					void start(MenuItem item) {
						mItem = item;

						super.start();
					}
				}.start(item);

				break;

			case R.id.add:
				if (!validateAll())
					break;

				intent.putExtra("name", ((EditText) findViewById(R.id.exittext_name)).getText().toString());
				intent.putExtra("host", ((EditText) findViewById(R.id.edittext_host)).getText().toString());
				intent.putExtra("icmp", ((CheckBox) findViewById(R.id.icmp)).isChecked());
				intent.putExtra("port", ((EditText) findViewById(R.id.port)).getText().toString());

				setResult(RESULT_OK, intent);
				finish();

				break;

			default:
				setResult(RESULT_CANCELED, intent);
				finish();
		}

		return super.onOptionsItemSelected(item);
	}

	private boolean validateAll() {
		boolean valid = true;

		if (!validateName())
			valid = false;

		if (!validateHost())
			valid = false;

		if (!validatePort())
			valid = false;

		return valid;
	}

	private boolean validateName() {
		if (mName.getText().toString().length() == 0) {
			mName.setError("Name is required.");

			return false;
		}

		ArrayList<String> existingNames = getIntent().getExtras().getStringArrayList("existingNames");

		if (existingNames.contains(mName.getText().toString())) {
			mName.setError("Name has already been used.");

			return false;
		}

		return true;
	}

	private boolean validateHost() {
		if (mHost.getText().toString().length() == 0) {
			mHost.setError("Host is required.");

			return false;
		}

		return true;
	}

	private boolean validatePort() {
		if (mICMP.isChecked())
			return true;

		if (mPort.length() > 0) {
			if (Integer.parseInt(mPort.getText().toString()) > 65535) {
				mPort.setError("Port must be in range 0 - 65535.");

				return false;
			}

			return true;
		}

		mPort.setError("Port is required, unless ICMP is used.");

		return false;
	}

	private void pingTest(MenuItem item) {
		class BeforePingRunnable implements Runnable {
			private MenuItem mItem;

			private BeforePingRunnable(MenuItem item) {
				mItem = item;
			}

			@Override
			public void run() {
				mItem.setEnabled(false);
				mName.setEnabled(false);
				mHost.setEnabled(false);
				mICMP.setEnabled(false);
				mPort.setEnabled(false);

				mTest.setVisibility(View.INVISIBLE);
				mPingProgressBar.setVisibility(View.VISIBLE);

				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mHost.getWindowToken(), 0);
			}
		}

		runOnUiThread(new BeforePingRunnable(item));

		PingResult result = null;

		if (mICMP.isChecked())
			result = ServerStatusPinger.pingICMP(mHost.getText().toString());

		else
			result = ServerStatusPinger.pingPort(mHost.getText().toString(), Integer.valueOf(mPort.getText().toString()));

		class AfterPintRunnable implements Runnable {
			private MenuItem mItem;
			private PingResult mResult;

			private AfterPintRunnable(MenuItem item, PingResult result) {
				mItem = item;
				mResult = result;
			}

			@Override
			public void run() {
				mStatus.setText(String.valueOf(mResult.statusCode.toString().toLowerCase()));

				if (mResult.statusCode == PingStatus.GOOD)
					mStatus.setTextColor(ContextCompat.getColor(mStatus.getContext(), R.color.statusBoxGood));

				else if (mResult.statusCode == PingStatus.BAD)
					mStatus.setTextColor(ContextCompat.getColor(mStatus.getContext(), R.color.statusBoxBad));

				else
					mStatus.setTextColor(ContextCompat.getColor(mStatus.getContext(), R.color.statusBoxUnknown));

				if (mResult.pingTime >= 0)
					mTime.setText(String.valueOf(mMsFormatter.format(mResult.pingTime)));

				else
					mTime.setText("N/A");

				((TextView) findViewById(R.id.statusDesc)).setText(mResult.status);

				mPingProgressBar.setVisibility(View.GONE);
				findViewById(R.id.linearLayout_test).setVisibility(View.VISIBLE);

				mItem.setEnabled(true);
				mName.setEnabled(true);
				mHost.setEnabled(true);
				mICMP.setEnabled(true);
				mPort.setEnabled(true);
			}
		}

		runOnUiThread(new AfterPintRunnable(item, result));
	}
}
