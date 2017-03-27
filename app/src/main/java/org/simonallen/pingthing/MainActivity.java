package org.simonallen.pingthing;

import android.content.Intent;
import android.gesture.Gesture;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener, View.OnLongClickListener, View.OnClickListener, OnPingResultListener {
	private final int mNewServerActivityCode = 0;
	private final int mNewWebsiteActivityCode = 1;
	private FlexboxLayout mStatusBoxContainer;
	private Pinger mPinger;
	private ArrayList<String> mPingerNames;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				super.onDrawerSlide(drawerView, 0);
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, 0);
			}
		};
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		mStatusBoxContainer = (FlexboxLayout) findViewById(R.id.status_box_container);

		mPingerNames = new ArrayList<>();
		mPinger = new Pinger();
		mPinger.setOnPingResultListener(this);
		mPinger.start();

		mStatusBoxContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				String type = (String) child.getTag(R.id.status_box_tag_type);
				String name = ((TextView) child.findViewById(R.id.name)).getText().toString();
				StatusPinger statusPinger = null;

				if (type.equals(getString(R.string.status_box_tag_type_server))) {
					statusPinger = new ServerStatusPinger(mPinger, child);
				} else if (type.equals(getString(R.string.status_box_tag_type_website))) {
					statusPinger = new WebsiteStatusPinger(mPinger, child);
				}

				if (statusPinger != null) {
					mPinger.add(statusPinger);
				}
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
				String name = ((TextView) child.findViewById(R.id.name)).getText().toString();

				mPinger.remove(name);
			}
		});
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_new_server) {
			Intent intent = new Intent(MainActivity.this, NewServerActivity.class);
			intent.putExtra("existingNames", mPingerNames);

			startActivityForResult(intent, mNewServerActivityCode);

			return true;
		} else if (id == R.id.action_new_website) {
			Intent intent = new Intent(MainActivity.this, NewWebsiteActivity.class);
			intent.putExtra("existingNames", mPingerNames);

			startActivityForResult(intent, mNewWebsiteActivityCode);

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if (id == R.id.nav_camera) {
			// Handle the camera action
		} else if (id == R.id.nav_gallery) {

		} else if (id == R.id.nav_slideshow) {

		} else if (id == R.id.nav_manage) {

		} else if (id == R.id.nav_settings) {

		} else if (id == R.id.nav_help) {

		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case mNewServerActivityCode:
					addServer(data.getExtras());

					break;

				case mNewWebsiteActivityCode:
					addWebsite(data.getExtras());

					break;
			}
		}
	}

	private void addServer(Bundle bundle) {
		FlexboxLayout container = (FlexboxLayout) findViewById(R.id.status_box_container);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View statusBox = inflater.inflate(R.layout.status_box_server, container, false);

		statusBox.setOnLongClickListener(this);
		statusBox.setOnClickListener(this);

		statusBox.setTag(R.id.status_box_tag_name, bundle.getString("name"));
		statusBox.setTag(R.id.status_box_tag_host, bundle.getString("host"));
		statusBox.setTag(R.id.status_box_tag_icmp, bundle.getBoolean("icmp"));
		statusBox.setTag(R.id.status_box_tag_port, bundle.getInt("port"));

		((TextView) statusBox.findViewById(R.id.name)).setText(bundle.getString("name"));
		((TextView) statusBox.findViewById(R.id.host)).setText(bundle.getString("host"));

		if (bundle.getBoolean("icmp"))
			((TextView) statusBox.findViewById(R.id.port)).setText("ICMP");

		else
			((TextView) statusBox.findViewById(R.id.port)).setText(String.valueOf(bundle.getInt("port")));

		mPingerNames.add(bundle.getString("name"));

		container.addView(statusBox);
	}

	private void addWebsite(Bundle bundle) {
		FlexboxLayout container = (FlexboxLayout) findViewById(R.id.status_box_container);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View statusBox = inflater.inflate(R.layout.status_box_website, container, false);

		statusBox.setOnLongClickListener(this);
		statusBox.setOnClickListener(this);

		statusBox.setTag(R.id.status_box_tag_name, bundle.getString("name"));
		statusBox.setTag(R.id.status_box_tag_follow_redirects, bundle.getBoolean("followRedirects"));
		statusBox.setTag(R.id.status_box_tag_follow_ssl_redirects, bundle.getBoolean("followSSLRedirects"));
		statusBox.setTag(R.id.status_box_tag_url, bundle.getString("url"));
		statusBox.setTag(R.id.status_box_tag_expected_status_codes, bundle.getIntArray("expectedStatusCodes"));

		((TextView) statusBox.findViewById(R.id.name)).setText(bundle.getString("name"));
		((TextView) statusBox.findViewById(R.id.url)).setText(bundle.getString("url"));

		StringBuilder stringBuilder = new StringBuilder();
		int[] statusCodes = bundle.getIntArray("expectedStatusCodes");

		if (statusCodes != null) {
			for (int statusCode : statusCodes) {
				stringBuilder.append(statusCode);
				stringBuilder.append(", ");
			}
		}

		((TextView) statusBox.findViewById(R.id.textView_expectedHTTPStatusCodes)).setText(stringBuilder.toString().replaceFirst(", $", ""));

		mPingerNames.add(bundle.getString("name"));

		container.addView(statusBox);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(MainActivity.this, StatusDetailActivity.class);

		intent.putExtra("name", (String)v.getTag(R.id.status_box_tag_name));

		startActivity(intent);
	}

	@Override
	public boolean onLongClick(View v) {
		v.setBackgroundResource(R.color.statusBoxSelected);

		return true;
	}

	@Override
	public void onPingResult() {

	}
}
