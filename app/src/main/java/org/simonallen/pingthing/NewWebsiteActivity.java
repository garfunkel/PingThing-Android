package org.simonallen.pingthing;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class NewWebsiteActivity extends AppCompatActivity {
	private String[] mAllStatusCodes;
	private boolean[] mSelectedItems;
	private boolean[] mUnconfirmedSelectedItems;
	private EditText mName;
	private CheckBox mFollowRedirects;
	private CheckBox mFollowSSLRedirects;
	private RadioGroup mProtocol;
	private RadioButton mProtocolHTTP;
	private RadioButton mProtocolHTTPS;
	private EditText mProtocolText;
	private EditText mURL;
	private EditText mExpectedStatusCodes;
	private TextView mStatus;
	private TextView mTime;
	private WebView mWeb;
	private ProgressBar mPingProgressBar;
	private LinearLayout mTest;
	private int[] mExpectedStatusIntCodes;
	private NumberFormat mMsFormatter = new DecimalFormat("#0.00");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_new_website);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_new);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		mName = (EditText) findViewById(R.id.exittext_name);
		mFollowRedirects = (CheckBox) findViewById(R.id.checkBox_followRedirects);
		mFollowSSLRedirects = (CheckBox) findViewById(R.id.checkBox_followSSLRedirects);
		mProtocol = (RadioGroup)findViewById(R.id.radioGroup_protocol);
		mProtocolHTTP = (RadioButton)findViewById(R.id.radioButton_http);
		mProtocolHTTPS = (RadioButton)findViewById(R.id.radioButton_https);
		mProtocolText = (EditText) findViewById(R.id.edittext_protocol);
		mURL = (EditText) findViewById(R.id.edittext_url);
		mExpectedStatusCodes = (EditText) findViewById(R.id.edittext_status_codes);
		mStatus = (TextView) findViewById(R.id.status);
		mTime = (TextView) findViewById(R.id.time);
		mWeb = (WebView) findViewById(R.id.web);
		mPingProgressBar = (ProgressBar) findViewById(R.id.progressBar_ping);
		mTest = (LinearLayout) findViewById(R.id.linearLayout_test);
		mAllStatusCodes = getResources().getStringArray(R.array.http_status_codes);
		mSelectedItems = new boolean[mAllStatusCodes.length];

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

		mExpectedStatusCodes.setCursorVisible(false);
		mExpectedStatusCodes.setLongClickable(false);

		mExpectedStatusCodes.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (v.hasFocus())
						handleSelectStatus(v);

					else
						v.requestFocus();
				}

				return true;
			}
		});

		mExpectedStatusCodes.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return keyCode != KeyEvent.KEYCODE_BACK;
			}
		});

		mExpectedStatusCodes.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					handleSelectStatus(v);
				}
			}
		});

		mExpectedStatusCodes.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
				return false;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode actionMode) {
			}
		});

		mProtocol.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
				if (checkedId == R.id.radioButton_http) {
					mProtocolText.setText("http://");
				} else {
					mProtocolText.setText("https://");
				}
			}
		});

		mWeb.setWebChromeClient(new WebChromeClient());
		mWeb.setWebViewClient(new WebViewClient());
		mWeb.getSettings().setJavaScriptEnabled(true);
		mWeb.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
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
				intent.putExtra("followRedirects", ((CheckBox)findViewById(R.id.checkBox_followRedirects)).isChecked());
				intent.putExtra("followSSLRedirects", ((CheckBox)findViewById(R.id.checkBox_followSSLRedirects)).isChecked());
				intent.putExtra("url", ((EditText) findViewById(R.id.edittext_protocol)).getText().toString() + ((EditText) findViewById(R.id.edittext_url)).getText().toString());
				intent.putExtra("expectedStatusCodes", mExpectedStatusIntCodes);

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

		if (!validateURL())
			valid = false;

		if (!validateExpectedStatusCodes())
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

	private boolean validateURL() {
		if (mURL.getText().toString().length() == 0) {
			mURL.setError("URL is required.");

			return false;
		}

		return true;
	}

	private boolean validateExpectedStatusCodes() {
		if (mExpectedStatusCodes.getText().toString().length() == 0) {
			mExpectedStatusCodes.setError("Expected status codes are required.");

			return false;
		}

		return true;
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
				mFollowRedirects.setEnabled(false);
				mFollowSSLRedirects.setEnabled(false);
				mProtocolHTTP.setEnabled(false);
				mProtocolHTTPS.setEnabled(false);
				mURL.setEnabled(false);
				mExpectedStatusCodes.setEnabled(false);

				mTest.setVisibility(View.INVISIBLE);
				mPingProgressBar.setVisibility(View.VISIBLE);
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mURL.getWindowToken(), 0);

				mWeb.loadUrl(mProtocolText.getText().toString() + mURL.getText().toString());
			}
		}

		runOnUiThread(new BeforePingRunnable(item));

		PingResult result = WebsiteStatusPinger.ping(mProtocolText.getText().toString() + mURL.getText().toString(), mFollowRedirects.isChecked(), mFollowSSLRedirects.isChecked(), mExpectedStatusIntCodes);

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
				mFollowRedirects.setEnabled(true);
				mFollowSSLRedirects.setEnabled(true);
				mProtocolHTTP.setEnabled(true);
				mProtocolHTTPS.setEnabled(true);
				mURL.setEnabled(true);
				mExpectedStatusCodes.setEnabled(true);
			}
		}

		runOnUiThread(new AfterPintRunnable(item, result));
	}

	private void handleSelectStatus(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		mUnconfirmedSelectedItems = mSelectedItems.clone();

		EditText et = (EditText) findViewById(R.id.edittext_status_codes);

		AlertDialog.Builder builder = new AlertDialog.Builder(et.getContext());
		builder.setTitle(R.string.select_expected_http_status_codes);
		builder.setMultiChoiceItems(R.array.http_status_codes, mUnconfirmedSelectedItems, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				mUnconfirmedSelectedItems[which] = isChecked;
			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mSelectedItems = mUnconfirmedSelectedItems;
				EditText et = (EditText) findViewById(R.id.edittext_status_codes);
				String value = "";

				for (int i = 0; i < mSelectedItems.length; i++) {
					if (mSelectedItems[i])
						value += mAllStatusCodes[i].substring(0, 3) + ", ";
				}

				if (value.endsWith(", "))
					value = value.substring(0, value.length() - 2);

				et.setText(value);

				String[] stringCodes = mExpectedStatusCodes.getText().toString().split(",");

				mExpectedStatusIntCodes = new int[stringCodes.length];

				for (int i = 0; i < stringCodes.length; i++) {
					mExpectedStatusIntCodes[i] = Integer.parseInt(stringCodes[i].trim());
				}

				mExpectedStatusCodes.setError(null);
			}
		});

		builder.show();
	}
}
