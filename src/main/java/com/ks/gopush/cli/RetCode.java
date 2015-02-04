package com.ks.gopush.cli;

public enum RetCode {
	SUCCESS(0), ILLEGAL_ARG(65534), INTERNAL_ERROR(65535), NOT_FOUND_NODE(1001), UNSUPPORT_SCHEMA(1003);
	RetCode(int code) {
		this.code = code;
	}

	private int code;

	public int getCode() {
		return code;
	}

	public static RetCode getRetCode(int code) {
		switch (code) {
		case 0:
			return SUCCESS;
		case 65534:
			return ILLEGAL_ARG;
		case 65535:
			return INTERNAL_ERROR;
		case 1001:
			return NOT_FOUND_NODE;
		case 1003:
			return UNSUPPORT_SCHEMA;
		default:
			throw new IllegalArgumentException("错误的code: " + code);
		}
	}
}
