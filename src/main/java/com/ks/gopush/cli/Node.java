package com.ks.gopush.cli;

public class Node {
	private String key;
	private String host;
	private int port;
	private long mid;
	private long pmid;

	public Node(String key, String host, int port) {
		this(key, host, port, 0L, 0L);
	}

	public Node(String key, String host, int port, long mid, long pmid) {
		this.key = key;
		this.host = host;
		this.port = port;
		if (mid < 0 || pmid < 0) {
			throw new IllegalArgumentException("mid与pmid必须大于等于s0");
		}
		this.mid = mid;
		this.pmid = pmid;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getMid() {
		return mid;
	}

	public long getPmid() {
		return pmid;
	}

	public boolean refreshMid(long mid) {
		if (this.mid < mid) {
			this.mid = mid;
			return true;
		}
		return false;
	}

	public boolean refreshPmid(long pmid) {
		if (this.pmid < pmid) {
			this.pmid = pmid;
			return true;
		}
		return false;
	}
}
