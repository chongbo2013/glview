package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class UnsubAckMessage extends MessageIDMessage {

	public UnsubAckMessage() {
		this.type = MQTT.MESSAGE_TYPE_UNSUBACK;
	}

}
