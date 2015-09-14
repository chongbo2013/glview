package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;

public class MQTTParser {

	private Map<String, Encoder> encoders = new HashMap<String, Encoder>();
	private Map<String, Decoder> decoders = new HashMap<String, Decoder>();

	public MQTTParser() {
	}

	public void registeEncoder(Encoder encoder) {
		String name = encoder.getClass().getSimpleName().replace("Encoder", "")
				.toUpperCase();
		encoders.put(name, encoder);
	}

	public void registeDecoder(Decoder encoder) {
		String name = encoder.getClass().getSimpleName().replace("Decoder", "")
				.toUpperCase();
		decoders.put(name, encoder);
	}

	/**
	 * encode to byte buffer
	 * @param message
	 * @return
	 */
	public ByteBuffer encode(Message message) {
		String name = message.getClass().getSimpleName().replace("Message", "")
				.toUpperCase();
		Encoder encoder = encoders.get(name);
		if (encoder == null) {
			throw new MQTTException("Encoder of name '" + name
					+ "' not registed");
		}
		return encoder.encode(message);
	}

	/**
	 * decode byte buffer
	 * @param buffer
	 * @return
	 */
	public Message decode(ByteBuffer buffer) {
		int pos = buffer.position();
		String name = MQTT.TYPES.get((byte) ((buffer.get() & 0xF0) >> 4));
		if (name == null) {
			throw new MQTTException("Message type error");
		}
		buffer.position(pos);
		Decoder decoder = decoders.get(name);
		if (decoder == null) {
			throw new MQTTException("Decoder of name '" + name
					+ "' not registed");
		}
		return decoder.decode(buffer);
	}

	/**
	 * check if the buffer decodable
	 * 
	 * @param buffer
	 * @return boolean
	 */
	public boolean decodable(ByteBuffer buffer) {
		int pos = buffer.position();
		String name = MQTT.TYPES.get((byte) ((buffer.get() & 0xF0) >> 4));
		if (name == null) {
			throw new MQTTException("Message type error");
		}
		buffer.position(pos);
		Decoder decoder = decoders.get(name);
		if (decoder == null) {
			throw new MQTTException("Decoder of name '" + name
					+ "' not registed");
		}
		return decoder.decodable(buffer);
	}
	
	public void clear() {
		encoders.clear();
		decoders.clear();
	}

}
