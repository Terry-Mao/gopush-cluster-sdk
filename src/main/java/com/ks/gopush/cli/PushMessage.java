package com.ks.gopush.cli;

public class PushMessage {

	/**
	 * 默认构造器
	 */
	public PushMessage() {

	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}

	public void setGid(int gid) {
		this.gid = gid;
	}

	public boolean isPub() {
		return gid == 0;
	}

	private String msg;
	private long mid;
	private int gid;
}
