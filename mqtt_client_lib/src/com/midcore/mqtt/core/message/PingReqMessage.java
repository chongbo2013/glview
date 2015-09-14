package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class PingReqMessage extends Message {

	public PingReqMessage() {
		this.type = MQTT.MESSAGE_TYPE_PINGREQ;
	}
}
