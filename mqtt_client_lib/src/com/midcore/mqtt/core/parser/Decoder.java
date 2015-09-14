package com.midcore.mqtt.core.parser;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;

public abstract class Decoder {

	public void decodeHeader(Message message, ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			throw new MQTTException("Protocol error - no data");
		}
		byte fixedHeader = buffer.get();
		message.setDup(((fixedHeader & 0x08) == 0x08));
		message.setQos((byte) ((fixedHeader & 0x06) >> 1));
		message.setRetain((fixedHeader & 0x01) != 0);
		message.setRemainLength(decodeRemainingLenght(buffer));
	}

	protected int decodeRemainingLenght(ByteBuffer buffer) {
		int multiplier = 1;
		int value = 0;
		byte digit;
		do {
			if (buffer.remaining() < 1) {
				return -1;
			}
			digit = buffer.get();
			value += (digit & 0x7F) * multiplier;
			multiplier *= 128;
		} while ((digit & 0x80) != 0);
		return value;
	}

	protected int decodeLength(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			throw new MQTTException("Protocol error - error length");
		}
		int msb = buffer.get() & 0xFF;
		int lsb = buffer.get() & 0xFF;
		return (msb << 8) | lsb;
	}
	
	protected int getMessageIdLength() {
		return 2;
	}
	protected int decodeMessageID(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			throw new MQTTException("Protocol error - error length");
		}
		int msb = buffer.get() & 0xFF;
		int lsb = buffer.get() & 0xFF;
		return (msb << 8) | lsb;
	}

	protected String decodeString(ByteBuffer buffer) {
		int length = decodeLength(buffer);
		if (length > buffer.remaining()) {
			throw new MQTTException("Protocol error - error data");
		}
		byte[] str = new byte[length];
		buffer.get(str);
		try {
			String result = new String(str, "UTF-8");
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new MQTTException(e);
		}
	}

	public abstract Message decode(ByteBuffer buffer);
	
	public boolean decodable(ByteBuffer buffer) {
		int pos = buffer.position();
		boolean result = doDecodable(buffer);
		buffer.position(pos);
		return result;
	}
	
	public abstract boolean doDecodable(ByteBuffer buffer);
}
