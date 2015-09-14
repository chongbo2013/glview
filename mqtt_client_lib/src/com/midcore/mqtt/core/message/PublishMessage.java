package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class PublishMessage extends MessageIDMessage {

	public PublishMessage() {
		this.type = MQTT.MESSAGE_TYPE_PUBLISH;
	}

	private String topic;

	private byte[] payload;

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	
}
