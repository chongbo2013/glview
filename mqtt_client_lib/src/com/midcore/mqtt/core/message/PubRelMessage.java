package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class PubRelMessage extends MessageIDMessage {

	public PubRelMessage() {
		this.type = MQTT.MESSAGE_TYPE_PUBREL;
		/* qos1 */
		setQos(MQTT.QOS_LEAST_ONCE);
	}

}
