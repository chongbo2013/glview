package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class DisconnectMessage extends Message {

	public DisconnectMessage() {
		this.type = MQTT.MESSAGE_TYPE_DISCONNECT;
	}
}
