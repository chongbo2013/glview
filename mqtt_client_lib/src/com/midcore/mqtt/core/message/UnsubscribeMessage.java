package com.midcore.mqtt.core.message;

import java.util.ArrayList;
import java.util.List;

import com.midcore.mqtt.core.MQTT;

public class UnsubscribeMessage extends MessageIDMessage {

	public UnsubscribeMessage() {
		this.type = MQTT.MESSAGE_TYPE_UNSUBSCRIBE;
		/* qos1 */
		setQos(MQTT.QOS_LEAST_ONCE);
	}

	private List<String> topics = new ArrayList<String>();
	
	public void addTopic(String topic) {
		topics.add(topic);
	}

	public List<String> getTopics() {
		return topics;
	}
	
}
