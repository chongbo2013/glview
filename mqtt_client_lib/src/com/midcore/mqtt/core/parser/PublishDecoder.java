package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PublishMessage;

public class PublishDecoder extends Decoder {

	@Override
	public Message decode(ByteBuffer buffer) {
		PublishMessage message = new PublishMessage();
		decodeHeader(message, buffer);
		int position = buffer.position();
		message.setTopic(decodeString(buffer));
		if (message.getQos() == MQTT.QOS_LEAST_ONCE
				|| message.getQos() == MQTT.QOS_ONCE) {
			if (buffer.remaining() < 4) {
				throw new MQTTException("Protocol error - error length");
			}
			message.setMessageID(decodeMessageID(buffer));
		}
        
		int payloadLength = message.getRemainLength()
				- (buffer.position() - position);
		if (payloadLength < 0 || payloadLength > buffer.remaining()) {
			throw new MQTTException(
					"Protocol error - error data remaining length");
		}
		byte[] payload = new byte[payloadLength];
		buffer.get(payload);
		message.setPayload(payload);
		return message;
	}

	@Override
	public boolean doDecodable(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			return false;
		}
		buffer.get();
		int remainingLength = decodeRemainingLenght(buffer);
		if (buffer.remaining() < remainingLength) {
			return false;
		}
		return true;
	}
}
