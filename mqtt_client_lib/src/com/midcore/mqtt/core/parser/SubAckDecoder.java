package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.SubAckMessage;

public class SubAckDecoder extends Decoder {

	@Override
	public Message decode(ByteBuffer buffer) {
		SubAckMessage message = new SubAckMessage();
		decodeHeader(message, buffer);
		int oldpos = buffer.position();
		message.setMessageID(decodeMessageID(buffer));
		int pos = buffer.position();
		for (int i = pos - oldpos; i < message.getRemainLength(); i++) {
			message.addQos(buffer.get());
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
