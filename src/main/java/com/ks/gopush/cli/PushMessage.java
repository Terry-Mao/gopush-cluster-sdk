package com.ks.gopush.cli;

public class PushMessage {
	
	public PushMessage(String msg, long mid, long gid) {
		this.msg = msg;
		this.mid = mid;
		this.gid = gid;
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
	
	public long getGid() {
		return gid;
	}
	
	public void setGid(long gid) {
		this.gid = gid;
	}
	
	private String msg;
	private long mid;
	private long gid;
}
