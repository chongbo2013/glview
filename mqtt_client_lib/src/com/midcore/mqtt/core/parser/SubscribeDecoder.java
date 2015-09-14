package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.SubscribeMessage;

public class SubscribeDecoder extends Decoder {

	@Override
	public Message decode(ByteBuffer buffer) {
		SubscribeMessage message = new SubscribeMessage();
		decodeHeader(message, buffer);
		int pos = buffer.position();
		message.setMessageID(decodeMessageID(buffer));
		while (message.getRemainLength() > buffer.position() - pos) {
			String topic = decodeString(buffer);
			if (buffer.remaining() <= 0) {
				throw new MQTTException("Protocol error - error data");
			}
			byte qos = buffer.get();
			message.addTopic(topic, qos);
		}
		return message;
	}

	@Override
	public boolean doDecodable(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			return false;
		}
		buffer.get();
		int remainingLength = decodeRemainingLenght(buffer);
		if (remainingLength < getMessageIdLength()) {
			throw new MQTTException("Protocol error - error data");
		}
		if (buffer.remaining() < remainingLength) {
			return false;
		}
		return true;
	}
}
