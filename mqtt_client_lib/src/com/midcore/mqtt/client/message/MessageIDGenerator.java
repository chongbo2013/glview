package com.midcore.mqtt.client.message;

public class MessageIDGenerator {

	int current = 0;

	public int next() {
		current = (++current) & 0xFF;
		return current;
	}

	public void reset() {
		current = 0;
	}

}
