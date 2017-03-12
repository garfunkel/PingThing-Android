package org.simonallen.pingthing;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;

public class NewWebsiteActivity extends AppCompatActivity {
	private String[] mAllStatusCodes;
	private boolean[] mSelectedItems;
	private AlertDialog mExpectedStatusCodesDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAllStatusCodes = getResources().getStringArray(R.array.http_status_codes);
		mSelectedItems = new boolean[mAllStatusCodes.length];

		setContentView(R.layout.activity_new_website);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_new);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		buildExpectedStatusCodesDialog();

		EditText et = (EditText) findViewById(R.id.edittext_status_codes);

		et.setCursorVisible(false);
		et.setLongClickable(false);

		et.setOnTouchListener(new View.OnTouchListener() {
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

		et.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return keyCode != KeyEvent.KEYCODE_BACK;
			}
		});

		et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					handleSelectStatus(v);
				}
			}
		});

		et.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
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
			case R.id.add:
				intent.putExtra("name", ((EditText)findViewById(R.id.exittext_name)).getText().toString());
				intent.putExtra("url", ((EditText)findViewById(R.id.edittext_url)).getText().toString());
				//intent.putExtra("statusCodes", )
				setResult(RESULT_OK, intent);
				break;

			default:
				setResult(RESULT_CANCELED, intent);
		}

		finish();

		return super.onOptionsItemSelected(item);
	}

	private void buildExpectedStatusCodesDialog() {
		EditText et = (EditText) findViewById(R.id.edittext_status_codes);

		AlertDialog.Builder builder = new AlertDialog.Builder(et.getContext());
		builder.setTitle(R.string.select_expected_http_status_codes);
		builder.setMultiChoiceItems(R.array.http_status_codes, mSelectedItems, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				mSelectedItems[which] = isChecked;
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText et = (EditText) findViewById(R.id.edittext_status_codes);
				String value = "";

				for (int i = 0; i < mSelectedItems.length; i++) {
					if (mSelectedItems[i])
						value += mAllStatusCodes[i].substring(0, 3) + ", ";
				}

				if (value.endsWith(", "))
					value = value.substring(0, value.length() - 2);

				et.setText(value);
			}
		});
		mExpectedStatusCodesDialog = builder.create();
	}

	private void handleSelectStatus(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

		mExpectedStatusCodesDialog.show();
	}
}
