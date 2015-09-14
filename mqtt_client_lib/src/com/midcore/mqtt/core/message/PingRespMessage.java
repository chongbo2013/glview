package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class PingRespMessage extends Message {

	public PingRespMessage() {
		this.type = MQTT.MESSAGE_TYPE_PINGRESP;
	}
}
