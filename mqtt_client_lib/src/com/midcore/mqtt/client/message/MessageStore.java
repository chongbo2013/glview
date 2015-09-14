package com.midcore.mqtt.client.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.midcore.mqtt.core.message.MessageIDMessage;

public class MessageStore {

	private Map<String, MessageIDMessage> qos2 = Collections
			.synchronizedMap(new HashMap<String, MessageIDMessage>());

	public void qos2(String name, MessageIDMessage message) {
		qos2.put(name, message);
	}

	public MessageIDMessage getQos2(String name) {
		return qos2.remove(name);
	}

	public void clear() {
		qos2.clear();
	}

}
