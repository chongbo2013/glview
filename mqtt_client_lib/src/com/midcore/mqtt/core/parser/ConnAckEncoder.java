package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.message.ConnAckMessage;
import com.midcore.mqtt.core.message.Message;

public class ConnAckEncoder extends Encoder {

	@Override
	public ByteBuffer encode(Message msg) {
		ConnAckMessage message = (ConnAckMessage)msg;
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put(encodeHeader(message));
		buffer.put((byte)2);
		buffer.put((byte)0);
		buffer.put(message.getAck());
		buffer.flip();
		return buffer;
	}
}
