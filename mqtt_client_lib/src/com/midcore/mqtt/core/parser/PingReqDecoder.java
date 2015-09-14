package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PingReqMessage;

public class PingReqDecoder extends Decoder {

	@Override
	public Message decode(ByteBuffer buffer) {
		PingReqMessage message = new PingReqMessage();
		decodeHeader(message, buffer);
		return message;
	}

	@Override
	public boolean doDecodable(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			return false;
		}
		buffer.get();
		int remainingLength = decodeRemainingLenght(buffer);
		if (remainingLength != 0) {
			throw new MQTTException("Protocol error - error data");
		}
		return true;
	}
}
