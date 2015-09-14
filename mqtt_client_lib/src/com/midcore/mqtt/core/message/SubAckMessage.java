package com.midcore.mqtt.core.message;

import java.util.ArrayList;
import java.util.List;

import com.midcore.mqtt.core.MQTT;

public class SubAckMessage extends MessageIDMessage {

	public SubAckMessage() {
		this.type = MQTT.MESSAGE_TYPE_SUBACK;
	}

	private List<Byte> qoss = new ArrayList<Byte>();

	public void addQos(byte qos) {
		this.qoss.add(qos);
	}

	public List<Byte> getQoss() {
		return qoss;
	}
}
