package org.simonallen.pingthing;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class NewServerActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_server);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_new);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		CheckBox icmp = (CheckBox)findViewById(R.id.icmp);

		icmp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				EditText port = (EditText)findViewById(R.id.port);

				if (isChecked) {
					port.setEnabled(false);
					port.setText("");
				} else {
					port.setEnabled(true);
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
			case R.id.add:
				intent.putExtra("name", ((EditText)findViewById(R.id.exittext_name)).getText().toString());
				intent.putExtra("host", ((EditText)findViewById(R.id.edittext_host)).getText().toString());
				//intent.putExtra("statusCodes", )
				setResult(RESULT_OK, intent);
				break;

			default:
				setResult(RESULT_CANCELED, intent);
		}

		finish();

		return super.onOptionsItemSelected(item);
	}
}
