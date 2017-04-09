package org.simonallen.pingthing;

import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.lang.Thread.sleep;

enum PingStatus {
	GOOD,
	BAD,
	UNKNOWN
}

interface StatusPinger extends Runnable {
	String getName();
	PingResult getResult();
	ArrayList<PingResult> getResultHistory();
}

interface OnPingResultListener{
	void onPingResult(StatusPinger pinger, PingResult result);
}

class PingResult implements Serializable {
	int rawStatusCode;
	PingStatus statusCode;
	String status;
	double pingTime;
	String data;
	Date date;

	PingResult() {
		rawStatusCode = -1;
		statusCode = PingStatus.UNKNOWN;
		status = "Unknown";
		pingTime = -1;
		data = "";
	}
}

class ServerStatusPinger implements StatusPinger {
	private static final String PING_CMD = "/system/bin/ping -c 1 %s";
	private static final Pattern MS_PATTERN = Pattern.compile("time=([\\d\\.]+)\\s+ms");
	private PingManager mPingManager;
	private String mName;
	private String mHost;
	private int mPort;
	private PingResult mResult = new PingResult();
	private ArrayList<PingResult> mResultHistory = new ArrayList<>();

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

				default:
					result.statusCode = PingStatus.BAD;
			}

			result.date = new Date();

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

			result.date = new Date();
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

	ServerStatusPinger(PingManager pingManager, String name, String host, int port) {
		mPingManager = pingManager;
		mName = name;
		mHost = host;
		mPort = port;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public PingResult getResult() {
		return mResult;
	}

	@Override
	public ArrayList<PingResult> getResultHistory() { return mResultHistory; }

	@Override
	public void run() {
		for (; ; ) {
			if (mPort == -1)
				mResult = pingICMP(mHost);

			else
				mResult = pingPort(mHost, mPort);

			mResultHistory.add(0, mResult);

			final ServerStatusPinger _this = this;

			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					OnPingResultListener listener = mPingManager.getOnPingResultListener();

					if (listener != null) {
						listener.onPingResult(_this, mResult);
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

class WebsiteStatusPinger implements StatusPinger {
	private PingManager mPingManager;
	private String mName;
	private String mUrl;
	private int[] mExpectedStatusCodes;
	private boolean mFollowRedirects;
	private boolean mFollowSSLRedirects;
	private PingResult mResult = new PingResult();
	private ArrayList<PingResult> mResultHistory = new ArrayList<>();
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

			result.date = new Date();
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

	WebsiteStatusPinger(PingManager pingManager, String name, String url, int[] expectedStatusCodes, boolean followRedirects, boolean followSSLRedirects) {
		mPingManager = pingManager;
		mName = name;
		mUrl = url;
		mExpectedStatusCodes = expectedStatusCodes;
		mFollowRedirects = followRedirects;
		mFollowSSLRedirects = followSSLRedirects;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public PingResult getResult() {
		return mResult;
	}

	@Override
	public ArrayList<PingResult> getResultHistory() { return mResultHistory; }

	@Override
	public void run() {
		for (; ; ) {
			mResult = ping(mUrl, mFollowRedirects, mFollowSSLRedirects, mExpectedStatusCodes);
			mResultHistory.add(0, mResult);

			final WebsiteStatusPinger _this = this;

			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					OnPingResultListener listener = mPingManager.getOnPingResultListener();

					if (listener != null) {
						listener.onPingResult(_this, mResult);
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

class PingManager extends Thread {
	private HashMap<String, StatusPinger> mStatusPingers;
	private HashMap<String, Thread> mStatusPingerThreads;
	private OnPingResultListener mOnPingResultListener;

	PingManager() {
		mStatusPingers = new HashMap<>();
		mStatusPingerThreads = new HashMap<>();
		mOnPingResultListener = null;
	}

	public void add(StatusPinger pinger) {
		Thread pingerThread = new Thread(pinger);

		mStatusPingers.put(pinger.getName(), pinger);
		mStatusPingerThreads.put(pinger.getName(), pingerThread);

		pingerThread.start();
	}

	public StatusPinger get(String name) {
		return mStatusPingers.get(name);
	}

	public void remove(String name) {
		Thread pingerThread = mStatusPingerThreads.get(name);

		if (pingerThread != null) {
			try {
				pingerThread.join();
			} catch (InterruptedException e) {

			}

			mStatusPingers.remove(name);
		}
	}

	public void setOnPingResultListener(OnPingResultListener listener) {
		mOnPingResultListener = listener;
	}

	public OnPingResultListener getOnPingResultListener() {
		return mOnPingResultListener;
	}

	@Override
	public void run() {

	}
}
