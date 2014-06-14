package com.ks.gopush.cli.exception;

@SuppressWarnings("serial")
public class GopushServerException extends Exception {
	public GopushServerException() {
		super();
	}

	public GopushServerException(String message) {
		super(message);
	}

	public GopushServerException(String message, Throwable t) {
		super(message, t);
	}
}
