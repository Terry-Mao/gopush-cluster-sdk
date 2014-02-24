package com.ks.gopush.cli.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

	public static String getURL(String protocol, String host, Integer port, String path, Object... query) {
		assert query != null && query.length % 2 == 0;

		StringBuilder result = new StringBuilder();
		result.append(protocol).append("://").append(host).append(":").append(port).append("/").append(path);
		if (query != null) {
			for (int i = 0; i < query.length; i += 2) {
				if (i == 0) {
					result.append("?");
				} else {
					result.append("&");
				}
				result.append(query[i]).append("=").append(query[i + 1]);
			}
		}

		return result.toString();
	}

	public static String get(String url) throws IOException {
		assert url != null && url.trim().length() != 0;

		HttpURLConnection huc = null;
		try {
			huc = getHttpURLConnection(url, "GET");
			BufferedReader reader = new BufferedReader(new InputStreamReader(huc.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.append(line);
				result.append("\r\n");
			}

			return result.toString();
		} finally {
			if (huc != null) {
				huc.disconnect();
			}
		}
	}

	public static String post(String url, String data) throws IOException {
		assert url != null && url.trim().length() != 0;

		HttpURLConnection huc = null;
		try {
			huc = getHttpURLConnection(url, "POST");

			huc.setDoOutput(true);
			huc.setDoInput(true);

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(huc.getOutputStream(), "UTF-8"));
			writer.write(data);

			BufferedReader reader = new BufferedReader(new InputStreamReader(huc.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.append(line);
				result.append("\r\n");
			}

			return result.toString();
		} finally {
			if (huc != null) {
				huc.disconnect();
			}
		}
	}

	private static HttpURLConnection getHttpURLConnection(String url, String method) throws IOException {
		HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();

		huc.setRequestMethod(method);
		huc.setUseCaches(false);
		huc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		huc.setRequestProperty("Charset", "UTF-8");

		return huc;
	}

	public static void main(String[] args) throws IOException {
		System.err.println(get("http://114.112.93.13/server/get?key=Terry-Mao&proto=2"));
	}
}
