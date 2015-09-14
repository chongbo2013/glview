package com.midcore.mqtt.core.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;

public abstract class Encoder {

	public byte encodeHeader(Message message) {
		byte b = 0;
		b = (byte) (0x00 | message.getType() * 0x10 /* bits 7-4 */| message
				.getQos() * 0x02 /* bits 2-1 */);
		if (message.isDup()) {
			/* bits 3 */
			b |= 0x08;
		}
		if (message.isRetain()) {
			/* bits 1 */
			b |= 0x01;
		}
		return b;
	}

	protected void encodeRemainLength(int remainLength, OutputStream out)
			throws IOException, MQTTException {
		if (remainLength > 268435455 || remainLength < 0) {
			throw new MQTTException(
					"Protocol error - remaining length too large");
		}
		do {
			int digit = (byte) (remainLength % 128);
			remainLength = remainLength / 128;
			if (remainLength > 0) {
				digit = (digit | 0x80);
			}
			out.write(digit);
		} while (remainLength > 0);
	}

	protected void encodeLength(int length, OutputStream out)
			throws IOException {
		out.write((byte) ((length & 0xFF00) >> 8)); // msb
		out.write((byte) (length & 0x00FF)); // lsb
	}

	protected void encodeLength(int length, ByteBuffer buffer)
			throws IOException {
		buffer.put((byte) ((length & 0xFF00) >> 8)); // msb
		buffer.put((byte) (length & 0x00FF)); // lsb
	}
	
	protected void encodeMessageID(Integer length, OutputStream out)
			throws IOException {
		out.write((byte) ((length & 0x0000FF00) >> 8)); 
		out.write((byte) (length & 0x000000FF)); 
	}

	protected void encodeMessageID(int length, ByteBuffer buffer)
			throws IOException {
		buffer.put((byte) ((length & 0xFF00) >> 8)); // msb
		buffer.put((byte) (length & 0x00FF)); // lsb
	}

	protected void encodeString(String string, OutputStream out)
			throws IOException {
		if (string == null) {
			throw new MQTTException(
					"Protocol error - error data");
		}
		byte[] bs = string.getBytes("UTF-8");
		encodeLength(bs.length, out);
		out.write(bs);
	}

	protected void encodeString(String string, ByteBuffer buffer)
			throws IOException {
		if (string == null) {
			throw new MQTTException(
					"Protocol error - error data");
		}
		byte[] bs = string.getBytes("UTF-8");
		encodeLength(bs.length, buffer);
		buffer.put(bs);
	}

	public abstract ByteBuffer encode(Message message);
}
