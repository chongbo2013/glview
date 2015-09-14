package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class PubAckMessage extends MessageIDMessage {

	public PubAckMessage() {
		this.type = MQTT.MESSAGE_TYPE_PUBACK;
	}

}
