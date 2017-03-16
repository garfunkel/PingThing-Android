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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum PingStatus {
	UP,
	DOWN,
	UNKNOWN
}

class PingResult {
	PingStatus statusCode;
	String status;
	double pingTime;

	PingResult() {
		statusCode = PingStatus.UNKNOWN;
		status = "Unknown";
		pingTime = -1;
	}
}

interface StatusPinger {
	void start();
}

class ServerStatusPinger extends Thread implements StatusPinger {
	private static final String PING_CMD = "/system/bin/ping -c 1 %s";
	private static final Pattern MS_PATTERN = Pattern.compile("time=([\\d\\.]+)\\s+ms");
	private View mStatusBox;
	private TextView mHostTextView;
	private TextView mStatusTextView;
	private TextView mPortTextView;
	private PingResult mResult = new PingResult();

	static PingResult pingICMP(String host) {
		Process process = null;
		String out = "";
		String err = "";
		String line = "";
		BufferedReader reader = null;
		PingResult result = new PingResult();

		try {
			process = Runtime.getRuntime().exec(String.format(PING_CMD, host));
			InputStream outStream = process.getInputStream();
			InputStream errStream = process.getErrorStream();

			switch (process.waitFor()) {
				case 0:
					result.statusCode = PingStatus.UP;

					break;

				case 1:
					result.statusCode = PingStatus.DOWN;

					break;

				default:
					result.statusCode = PingStatus.UNKNOWN;
			}

			// Read stdout into string.
			reader = new BufferedReader(new InputStreamReader(outStream));

			while ((line = reader.readLine()) != null) {
				out += line + "\n";
			}

			out = out.trim();

			// Read stderr into string.
			reader = new BufferedReader(new InputStreamReader(errStream));

			while ((line = reader.readLine()) != null) {
				err += line + "\n";
			}

			err = err.trim();

			if (err.isEmpty())
				result.status = out;

			else
				result.status = err;

			// Get ping time.
			if (result.statusCode == PingStatus.UP) {
				Matcher matcher = MS_PATTERN.matcher(out);

				if (matcher.find()) {
					result.pingTime = Float.parseFloat(matcher.group(1));
				}
			}
		} catch (Exception e) {
		}

		return result;
	}

	static PingResult pingPort(String host, int port) {
		PingResult result = new PingResult();

		try {
			SocketAddress sockaddr = new InetSocketAddress(host, port);
			// Create an unbound socket
			Socket sock = new Socket();

			long startTime = System.nanoTime();

			sock.connect(sockaddr, 10000);

			long endTime = System.nanoTime();

			result.statusCode = PingStatus.UP;
			result.status = "OK";
			result.pingTime = (endTime - startTime) / 1000000.0;
		} catch (UnknownHostException e) {
			result.statusCode = PingStatus.DOWN;
			result.status = "Unknown host: " + e.getMessage();
		} catch (Exception e) {
			result.statusCode = PingStatus.DOWN;
			result.status = "Cannot connect to host: " + e.getMessage();
		}

		return result;
	}

	ServerStatusPinger(View statusBox) {
		mStatusBox = statusBox;
		mHostTextView = (TextView) mStatusBox.findViewById(R.id.host);
		mStatusTextView = (TextView) mStatusBox.findViewById(R.id.status);
		mPortTextView = (TextView) mStatusBox.findViewById(R.id.port);
	}

	@Override
	public void run() {
		PingResult pingResult;

		for (; ; ) {
			if (mPortTextView.getText().toString().equals("ICMP"))
				mResult = pingICMP(mHostTextView.getText().toString());

			else
				mResult = pingPort(mHostTextView.getText().toString(), Integer.valueOf(mPortTextView.getText().toString()));

			mStatusBox.post(new Runnable() {
				@Override
				public void run() {
					mStatusTextView.setText(mResult.status);

					if (mResult.statusCode == PingStatus.UP) {
						mStatusBox.setBackgroundResource(R.color.statusBoxGood);
					} else if (mResult.statusCode == PingStatus.DOWN) {
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
				String type = (String) child.getTag(R.id.status_box_tag_type);
				String name = ((TextView) child.findViewById(R.id.name)).getText().toString();
				StatusPinger statusPinger = null;

				if (type.equals(mContainer.getContext().getResources().getString(R.string.status_box_tag_type_server))) {
					statusPinger = new ServerStatusPinger(child);
				} else if (type.equals(mContainer.getContext().getString(R.string.status_box_tag_type_website))) {
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
