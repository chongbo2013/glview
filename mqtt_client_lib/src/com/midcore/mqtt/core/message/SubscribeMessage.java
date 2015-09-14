package com.midcore.mqtt.core.message;

import java.util.ArrayList;
import java.util.List;

import com.midcore.mqtt.core.MQTT;

public class SubscribeMessage extends MessageIDMessage {

	public SubscribeMessage() {
		this.type = MQTT.MESSAGE_TYPE_SUBSCRIBE;
		/* qos1 */
		setQos(MQTT.QOS_LEAST_ONCE);
	}

	private List<Topic> topics = new ArrayList<Topic>();

	public void addTopic(String topic, byte qos) {
		topics.add(new Topic(topic, qos));
	}

	public List<Topic> getTopics() {
		return topics;
	}

	public static class Topic {
		String topic;
		byte qos = 0;

		public Topic(String topic, byte qos) {
			super();
			this.topic = topic;
			this.qos = qos;
		}

		public String getTopic() {
			return topic;
		}

		public void setTopic(String topic) {
			this.topic = topic;
		}

		public byte getQos() {
			return qos;
		}

		public void setQos(byte qos) {
			this.qos = qos;
		}

	}
}
