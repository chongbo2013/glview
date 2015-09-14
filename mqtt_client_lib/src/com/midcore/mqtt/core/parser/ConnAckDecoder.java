package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.ConnAckMessage;
import com.midcore.mqtt.core.message.Message;

public class ConnAckDecoder extends Decoder {

	@Override
	public Message decode(ByteBuffer buffer) {
		ConnAckMessage message = new ConnAckMessage();
		decodeHeader(message, buffer);
		buffer.get();
		message.setAck(buffer.get());
		return message;
	}

	@Override
	public boolean doDecodable(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			return false;
		}
		buffer.get();
		int remainingLength = decodeRemainingLenght(buffer);
		if (remainingLength != 2) {
			throw new MQTTException("Protocol error - error data");
		}
		if (buffer.remaining() < remainingLength) {
			return false;
		}
		return true;
	}

}
