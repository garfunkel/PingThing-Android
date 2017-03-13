package org.simonallen.pingthing;

import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by simon on 12/03/17.
 */

enum PingStatus {
	UP,
	DOWN,
	UNKNOWN
}

interface StatusPinger {
	void start();
}

class ServerStatusPinger extends Thread implements StatusPinger {
	private static final String PING_CMD = "/system/bin/ping -c 1 %s";
	private View mStatusBox;
	private TextView mHostTextView;
	private TextView mStatusTextView;
	private PingStatus mStatusCode = PingStatus.UNKNOWN;
	private String mStatus = "Unknown";

	ServerStatusPinger(View statusBox) {
		mStatusBox = statusBox;
		mHostTextView = (TextView) mStatusBox.findViewById(R.id.host);
		mStatusTextView = (TextView) mStatusBox.findViewById(R.id.status);
	}

	@Override
	public void run() {
		for (; ; ) {
			Process process = null;
			String out = "";
			String err = "";
			String line = "";
			BufferedReader reader = null;

			try {
				process = Runtime.getRuntime().exec(String.format(PING_CMD, mHostTextView.getText().toString()));
				InputStream outStream = process.getInputStream();
				InputStream errStream = process.getErrorStream();

				switch (process.waitFor()) {
					case 0:
						mStatusCode = PingStatus.UP;

						break;

					case 1:
						mStatusCode = PingStatus.DOWN;

						break;

					default:
						mStatusCode = PingStatus.UNKNOWN;
				}

				// Read stdout into string.
				reader = new BufferedReader(new InputStreamReader(outStream));

				while ((line = reader.readLine()) != null) {
					out += line + " ";
				}

				out = out.trim();

				// Read stderr into string.
				reader = new BufferedReader(new InputStreamReader(errStream));

				while ((line = reader.readLine()) != null) {
					err += line + " ";
				}

				err = err.trim();

				if (err.isEmpty())
					mStatus = out;

				else
					mStatus = err;

				mStatusBox.post(new Runnable() {
					@Override
					public void run() {
						mStatusTextView.setText(mStatus);

						if (mStatusCode == PingStatus.UP) {
							mStatusBox.setBackgroundResource(R.color.statusBoxGood);
						} else if (mStatusCode == PingStatus.DOWN) {
							mStatusBox.setBackgroundResource(R.color.statusBoxBad);
						} else {
							mStatusBox.setBackgroundResource(R.color.statusBoxUnknown);
						}
					}
				});
			} catch (Exception e) {
			}

			try {
				sleep(10000);
			} catch (InterruptedException e) {
			}
		}
	}
}

class WebsiteStatusPinger extends Thread implements StatusPinger {
	private View mStatusBox;
	private TextView mUrlTextView;
	private TextView mStatusTextView;
	private PingStatus mStatusCode = PingStatus.UNKNOWN;
	private String mStatus = "Unknown";

	WebsiteStatusPinger(View statusBox) {
		mStatusBox = statusBox;
		mUrlTextView = (TextView) mStatusBox.findViewById(R.id.url);
		mStatusTextView = (TextView) mStatusBox.findViewById(R.id.status);
	}

	@Override
	public void run() {
		for (; ; ) {
			Log.d("HERE", "HERE");
			try {
				SocketAddress sockaddr = new InetSocketAddress(mUrlTextView.getText().toString(), 80);
				// Create an unbound socket
				Socket sock = new Socket();

				sock.connect(sockaddr, 10000);
				mStatusCode = PingStatus.UP;
				mStatus = "OK";
			} catch (Exception e) {
				mStatusCode = PingStatus.DOWN;
				mStatus = e.getMessage();
				Log.e("ERROR", e.toString());
			}

			mStatusBox.post(new Runnable() {
				@Override
				public void run() {
					mStatusTextView.setText(mStatus);

					if (mStatusCode == PingStatus.UP) {
						mStatusBox.setBackgroundResource(R.color.statusBoxGood);
					} else if (mStatusCode == PingStatus.DOWN) {
						mStatusBox.setBackgroundResource(R.color.statusBoxBad);
					} else {
						mStatusBox.setBackgroundResource(R.color.statusBoxUnknown);
					}
				}
			});

			try {
				sleep(10000);
			} catch (InterruptedException e) {
			}
		}
	}
}


class Pinger extends Thread {
	private FlexboxLayout mContainer;
	private HashMap<String, StatusPinger> mStatusPingers;

	Pinger(FlexboxLayout container) {
		mContainer = container;
		mStatusPingers = new HashMap<>();

		mContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				String type = (String) child.getTag();
				String name = ((TextView) child.findViewById(R.id.name)).getText().toString();
				StatusPinger statusPinger = null;

				if (type.equals("SERVER")) {
					statusPinger = new ServerStatusPinger(child);
				} else if (type.equals("WEBSITE")) {
					statusPinger = new WebsiteStatusPinger(child);
				}

				if (statusPinger != null) {
					mStatusPingers.put(name, statusPinger);

					statusPinger.start();
				}
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
				String type = (String) child.getTag();
				String name = ((TextView) child.findViewById(R.id.name)).getText().toString();
				StatusPinger statusPinger = mStatusPingers.get(name);

				if (statusPinger != null) {
					Thread statusPingerThread = (Thread) statusPinger;

					try {
						statusPingerThread.join();
					} catch (InterruptedException e) {

					}
				}
			}
		});
	}

	@Override
	public void run() {

	}
}
