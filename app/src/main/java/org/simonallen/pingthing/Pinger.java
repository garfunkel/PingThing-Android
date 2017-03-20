package org.simonallen.pingthing;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

enum PingStatus {
	GOOD,
	BAD,
	UNKNOWN
}

class PingResult {
	int rawStatusCode;
	PingStatus statusCode;
	String status;
	double pingTime;
	String data;

	PingResult() {
		rawStatusCode = -1;
		statusCode = PingStatus.UNKNOWN;
		status = "Unknown";
		pingTime = -1;
		data = "";
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

			result.rawStatusCode = process.waitFor();

			switch (result.rawStatusCode) {
				case 0:
					result.statusCode = PingStatus.GOOD;

					break;

				case 1:
					result.statusCode = PingStatus.BAD;

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
			if (result.statusCode == PingStatus.GOOD) {
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

			result.statusCode = PingStatus.GOOD;
			result.status = "OK";
			result.pingTime = (endTime - startTime) / 1000000.0;
		} catch (UnknownHostException e) {
			result.statusCode = PingStatus.BAD;
			result.status = "Unknown host: " + e.getMessage();
		} catch (Exception e) {
			result.statusCode = PingStatus.BAD;
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
		for (; ; ) {
			if (mPortTextView.getText().toString().equals("ICMP"))
				mResult = pingICMP(mHostTextView.getText().toString());

			else
				mResult = pingPort(mHostTextView.getText().toString(), Integer.valueOf(mPortTextView.getText().toString()));

			mStatusBox.post(new Runnable() {
				@Override
				public void run() {
					mStatusTextView.setText(mResult.status);

					if (mResult.statusCode == PingStatus.GOOD) {
						mStatusBox.setBackgroundResource(R.color.statusBoxGood);
					} else if (mResult.statusCode == PingStatus.BAD) {
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
	private TextView mURLTextView;
	private TextView mExpectedStatusTextView;
	private TextView mStatusTextView;
	private PingResult mResult = new PingResult();
	private final static SparseArray<String> HTTPStatusCodeMap = new SparseArray<String>();

	static {
		HTTPStatusCodeMap.append(100, "Continue");
		HTTPStatusCodeMap.append(101, "Switching Protocol");
		HTTPStatusCodeMap.append(200, "OK");
		HTTPStatusCodeMap.append(201, "Created");
		HTTPStatusCodeMap.append(202, "Accepted");
		HTTPStatusCodeMap.append(203, "Non-Authoritative Information");
		HTTPStatusCodeMap.append(204, "No Content");
		HTTPStatusCodeMap.append(205, "Reset Content");
		HTTPStatusCodeMap.append(206, "Partial Content");
		HTTPStatusCodeMap.append(300, "Multiple Choices");
		HTTPStatusCodeMap.append(301, "Moved Permanently");
		HTTPStatusCodeMap.append(302, "Found");
		HTTPStatusCodeMap.append(303, "See Other");
		HTTPStatusCodeMap.append(304, "Not Modified");
		HTTPStatusCodeMap.append(307, "Temporary Redirect");
		HTTPStatusCodeMap.append(308, "Permanent Redirect");
		HTTPStatusCodeMap.append(400, "Bad Request");
		HTTPStatusCodeMap.append(401, "Unauthorized");
		HTTPStatusCodeMap.append(403, "Forbidden");
		HTTPStatusCodeMap.append(404, "Not Found");
		HTTPStatusCodeMap.append(405, "Method Not Allowed");
		HTTPStatusCodeMap.append(406, "Not Acceptable");
		HTTPStatusCodeMap.append(407, "Proxy Authentication Required");
		HTTPStatusCodeMap.append(408, "Request Timeout");
		HTTPStatusCodeMap.append(409, "Conflict");
		HTTPStatusCodeMap.append(410, "Gone");
		HTTPStatusCodeMap.append(411, "Length Required");
		HTTPStatusCodeMap.append(412, "Precondition Failed");
		HTTPStatusCodeMap.append(413, "Payload Too Large");
		HTTPStatusCodeMap.append(414, "URI Too Long");
		HTTPStatusCodeMap.append(415, "Unsupported Media Type");
		HTTPStatusCodeMap.append(416, "Range Not Satisfiable");
		HTTPStatusCodeMap.append(417, "Expectation Failed");
		HTTPStatusCodeMap.append(426, "Upgrade Required");
		HTTPStatusCodeMap.append(428, "Precondition Required");
		HTTPStatusCodeMap.append(429, "Too Many Requests");
		HTTPStatusCodeMap.append(431, "Request Header Fields Too Large");
		HTTPStatusCodeMap.append(451, "Unavailable For Legal Reasons");
		HTTPStatusCodeMap.append(500, "Internal Server Error");
		HTTPStatusCodeMap.append(501, "Not Implemented");
		HTTPStatusCodeMap.append(502, "Bad Gateway");
		HTTPStatusCodeMap.append(503, "Service Unavailable");
		HTTPStatusCodeMap.append(504, "Gateway Timeout");
		HTTPStatusCodeMap.append(505, "HTTP Version Not Supported");
		HTTPStatusCodeMap.append(511, "Network Authentication Required");
	}

	static PingResult ping(String url, boolean followRedirects, boolean followSSLRedirects, int[] expectedStatuses) {
		PingResult result = new PingResult();
		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

		clientBuilder.followRedirects(followRedirects);
		clientBuilder.followSslRedirects(followSSLRedirects);
		OkHttpClient client = clientBuilder.build();

		try {
			Request request = new Request.Builder().url(url).build();
			Response response;

			long startTime = System.nanoTime();

			response = client.newCall(request).execute();

			result.rawStatusCode = response.code();
			result.statusCode = PingStatus.BAD;

			for (int code : expectedStatuses) {
				if (result.rawStatusCode == code)
					result.statusCode = PingStatus.GOOD;
			}

			if (response.message() != null && !response.message().isEmpty())
				result.status = String.format("%s (%s)", String.valueOf(response.code()), response.message());

			else if (HTTPStatusCodeMap.get(response.code()) != null)
				result.status = String.format("%s (%s)", String.valueOf(response.code()), HTTPStatusCodeMap.get(response.code()));

			else
				result.status = "Invalid HTTP response code.";

			result.data = response.body().toString();
			result.pingTime = (System.nanoTime() - startTime) / 1000000.0;
		} catch (Exception e) {
			result.status = e.getMessage();
			result.statusCode = PingStatus.BAD;
		}

		return result;
	}

	WebsiteStatusPinger(View statusBox) {
		mStatusBox = statusBox;
		mURLTextView = (TextView) mStatusBox.findViewById(R.id.url);
		mExpectedStatusTextView = (TextView) mStatusBox.findViewById(R.id.textView_expectedHTTPStatusCodes);
		mStatusTextView = (TextView) mStatusBox.findViewById(R.id.status);
	}

	@Override
	public void run() {
		for (; ; ) {
			mResult = ping((String)mStatusBox.getTag(R.id.status_box_tag_url), (boolean)mStatusBox.getTag(R.id.status_box_tag_follow_redirects), (boolean)mStatusBox.getTag(R.id.status_box_tag_follow_ssl_redirects), (int[])mStatusBox.getTag(R.id.status_box_tag_expected_status_codes));

			mStatusBox.post(new Runnable() {
				@Override
				public void run() {
					mStatusTextView.setText(mResult.status);

					if (mResult.statusCode == PingStatus.GOOD) {
						mStatusBox.setBackgroundResource(R.color.statusBoxGood);
					} else if (mResult.statusCode == PingStatus.BAD) {
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
