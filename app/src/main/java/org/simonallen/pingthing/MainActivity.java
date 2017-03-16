package org.simonallen.pingthing;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {
	private final int mNewServerActivityCode = 0;
	private final int mNewWebsiteActivityCode = 1;
	private Pinger pinger;
	private ArrayList<String> mPingerNames;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton newServerFab = (FloatingActionButton) findViewById(R.id.fab_add_server);
		newServerFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(MainActivity.this, NewServerActivity.class);
				intent.putExtra("existingNames", mPingerNames);

				startActivityForResult(intent, mNewServerActivityCode);

				FloatingActionMenu fab = (FloatingActionMenu) findViewById(R.id.fab);
				fab.close(true);
			}
		});

		newServerFab = (FloatingActionButton) findViewById(R.id.fab_add_website);
		newServerFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(MainActivity.this, NewWebsiteActivity.class);
				intent.putExtra("existingNames", mPingerNames);

				startActivityForResult(intent, mNewWebsiteActivityCode);

				FloatingActionMenu fab = (FloatingActionMenu) findViewById(R.id.fab);
				fab.close(true);
			}
		});

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

		FlexboxLayout statusBoxContainer = (FlexboxLayout) findViewById(R.id.status_box_container);
		mPingerNames = new ArrayList<>();
		pinger = new Pinger(statusBoxContainer);

		pinger.start();
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
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
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

		((TextView) statusBox.findViewById(R.id.name)).setText(bundle.getString("name"));
		((TextView) statusBox.findViewById(R.id.host)).setText(bundle.getString("host"));

		if (bundle.getBoolean("icmp"))
			((TextView) statusBox.findViewById(R.id.port)).setText("ICMP");

		else
			((TextView) statusBox.findViewById(R.id.port)).setText(bundle.getString("port"));

		mPingerNames.add(bundle.getString("name"));

		container.addView(statusBox);
	}

	private void addWebsite(Bundle bundle) {
		FlexboxLayout container = (FlexboxLayout) findViewById(R.id.status_box_container);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View statusBox = inflater.inflate(R.layout.status_box_website, container, false);

		((TextView) statusBox.findViewById(R.id.name)).setText(bundle.getString("name"));
		((TextView) statusBox.findViewById(R.id.url)).setText(bundle.getString("url"));

		mPingerNames.add(bundle.getString("name"));

		container.addView(statusBox);
	}
}
