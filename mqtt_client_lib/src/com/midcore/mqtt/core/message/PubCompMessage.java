package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class PubCompMessage extends MessageIDMessage {

	public PubCompMessage() {
		this.type = MQTT.MESSAGE_TYPE_PUBCOMP;
	}

}
