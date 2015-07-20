package com.glview.exception;

public class GLViewRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6717995842546239327L;

	public GLViewRuntimeException() {
	}

	public GLViewRuntimeException(String detailMessage) {
		super(detailMessage);
	}

	public GLViewRuntimeException(Throwable throwable) {
		super(throwable);
	}

	public GLViewRuntimeException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
