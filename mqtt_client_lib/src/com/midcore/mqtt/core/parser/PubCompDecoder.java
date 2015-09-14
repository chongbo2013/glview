package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PubCompMessage;

public class PubCompDecoder extends Decoder {

	@Override
	public Message decode(ByteBuffer buffer) {
		PubCompMessage message = new PubCompMessage();
		decodeHeader(message, buffer);
		message.setMessageID(decodeMessageID(buffer));
		return message;
	}
	
	@Override
	public boolean doDecodable(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			return false;
		}
		buffer.get();
		int remainingLength = decodeRemainingLenght(buffer);
		if (remainingLength != getMessageIdLength()) {
			throw new MQTTException("Protocol error - error data");
		}
		if (buffer.remaining() < remainingLength) {
			return false;
		}
		return true;
	}

}
