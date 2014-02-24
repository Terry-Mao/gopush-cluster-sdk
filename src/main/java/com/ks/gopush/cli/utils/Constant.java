package com.ks.gopush.cli.utils;

public class Constant {
	public static final int KS_NET_STATE_OK = 0;

	public static final int KS_NET_EXCEPTION_SUBSCRIBE_CODE = -1;
	public static final int KS_NET_EXCEPTION_OFFLINE_CODE = -2;
	public static final int KS_NET_EXCEPTION_SOCKET_READ_CODE = -3;
	public static final int KS_NET_EXCEPTION_SOCKET_WRITE_CODE = -4;
	public static final int KS_NET_EXCEPTION_SOCKET_INIT_CODE = -5;

	public static final String KS_NET_JSON_KEY_RET = "ret";
	public static final String KS_NET_JSON_KEY_MSG = "msg";
	public static final String KS_NET_JSON_KEY_DATA = "data";
	public static final String KS_NET_JSON_KEY_SERVER = "server";
	
	public static final String KS_NET_JSON_KEY_MESSAGES = "msgs";
	public static final String KS_NET_JSON_KEY_PMESSAGES = "pmsgs";
	
	public static final String KS_NET_JSON_KEY_MESSAGE_MSG = "msg";
	public static final String KS_NET_JSON_KEY_MESSAGE_MID = "mid";
	public static final String KS_NET_JSON_KEY_MESSAGE_GID = "gid";

	public static final String KS_NET_KEY_ADDRESS = "address";
	public static final String KS_NET_KEY_PORT = "port";

	public static final String KS_NET_SOCKET_CONNECTION_ACTION = "socket_connection_action";

	public static final int KS_NET_MESSAGE_OBTAIN_DATA_OK = 2;
	public static final int KS_NET_MESSAGE_DISCONNECT = 1;
}
