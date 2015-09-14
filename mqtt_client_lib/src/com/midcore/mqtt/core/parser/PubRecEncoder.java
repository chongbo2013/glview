package com.midcore.mqtt.core.parser;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PubRecMessage;

public class PubRecEncoder extends Encoder {

	@Override
	public ByteBuffer encode(Message msg) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(6);
			PubRecMessage message = (PubRecMessage) msg;
			buffer.put(encodeHeader(message));
			buffer.put((byte) 0x02);
			encodeMessageID(message.getMessageID(), buffer);
			buffer.flip();
			return buffer;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
