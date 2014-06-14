package com.ks.gopush.cli;

public enum Proto {
	WEBSOCKET(1), TCP(2);
	Proto(int proto) {
		this.proto = proto;
	}

	private int proto;

	public int getProto() {
		return proto;
	}
}
