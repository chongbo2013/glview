package com.midcore.mqtt.core;

public class MQTTException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MQTTException(String message) {
		super(message);
	}

	public MQTTException() {
		super();
	}

	public MQTTException(String message, Throwable cause) {
		super(message, cause);
	}

	public MQTTException(Throwable cause) {
		super(cause);
	}

}
