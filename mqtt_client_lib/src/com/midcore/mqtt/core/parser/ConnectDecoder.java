package com.midcore.mqtt.core.parser;

import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.ConnectMessage;
import com.midcore.mqtt.core.message.Message;

public class ConnectDecoder extends Decoder {

	@Override
	public Message decode(ByteBuffer buffer) {
		try {
			ConnectMessage message = new ConnectMessage();
			decodeHeader(message, buffer);
			
			int pos = buffer.position();
			
			byte[] version = MQTT.VERSION.getBytes("UTF-8");
			for (int i = 0; i < version.length; i++) {
				if (version[i] != buffer.get()) {
					/* invalid version */
					message.setAck((byte)1);
					return message;
				}
			}
			/* connection flags */
			byte connectionFlags = buffer.get();
			message.setHasUsername(((connectionFlags & 0x80) != 0));
			message.setHasPassword(((connectionFlags & 0x40) != 0));
			message.setWillRetain(((connectionFlags & 0x20) != 0));
			message.setWillQos((byte)((connectionFlags & 0x18) >> 3));
			message.setWillFlag(((connectionFlags & 0x04) != 0));
			message.setCleanSession(((connectionFlags & 0x02) != 0));
			/* keepAlive */
			message.setKeepAlive(decodeLength(buffer));
			
			if (message.getRemainLength() == 12) {
			    message.setAck((byte)0);
			    return message;
			}
			
			String clientID = decodeString(buffer);
			if (clientID.length() > 23) {
				/* identifier rejected */
				message.setAck((byte) 2);
				return message;
			}
			message.setClientID(clientID);
			
			if (message.isWillFlag()) {
				message.setWillTopic(decodeString(buffer));
				message.setWillMessage(decodeString(buffer));
			}
		    
			message.setAck((byte) 0);
			/* username/password */
		    /* remaining length has precedence over the user and password flags */
			if ((buffer.position() - pos) == message.getRemainLength()) {
				return message;
			}
			if (message.isHasUsername()) {
				message.setUsername(decodeString(buffer));
			}
			if ((buffer.position() - pos) == message.getRemainLength()) {
				return message;
			}
			if (message.isHasPassword()) {
				message.setPassword(decodeString(buffer));
			}
			
			return message;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean doDecodable(ByteBuffer buffer) {
		if (buffer.remaining() < 2) {
			return false;
		}
		buffer.get();
		int remainingLength = decodeRemainingLenght(buffer);
		if (remainingLength < 12) {
			throw new MQTTException("Protocol error - error data");
		}
		if (buffer.remaining() < remainingLength) {
			return false;
		}
		return true;
	}

}
