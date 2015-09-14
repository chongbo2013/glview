package com.midcore.mqtt.core.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.message.ConnectMessage;
import com.midcore.mqtt.core.message.Message;

public class ConnectEncoder extends Encoder {

	@Override
	public ByteBuffer encode(Message msg) {
		ByteArrayOutputStream out = null;
		ByteArrayOutputStream data = null;
		try {
			out = new ByteArrayOutputStream();
			data = new ByteArrayOutputStream();
			ConnectMessage message = (ConnectMessage) msg;
			/* variable header */
			/* version */
			data.write(MQTT.VERSION.getBytes("UTF-8"));
			/* connection flags */
			byte connectionFlags = 0;
			if (message.isHasUsername()) {
				connectionFlags |= 0x080;
			}
			if (message.isHasPassword()) {
				connectionFlags |= 0x040;
			}
			if (message.isWillFlag()) {
				connectionFlags |= 0x04;
			}
			connectionFlags |= ((message.getWillQos() & 0x03) << 3);
			if (message.isWillRetain()) {
				connectionFlags |= 0x020;
			}
			if (message.isCleanSession()) {
				connectionFlags |= 0x02;
			}
			data.write(connectionFlags);
			/* keep alive timer */
			encodeLength(message.getKeepAlive(), data);
			/* variable part */
			if (message.getClientID() != null) {
				encodeString(message.getClientID(), data);
				
				if (message.isWillFlag()) {
					encodeString(message.getWillTopic(), data);
					encodeString(message.getWillMessage(), data);
				}
				
				if (message.isHasUsername() && message.getUsername() != null) {
					encodeString(message.getUsername(), data);
				}
				if (message.isHasPassword() && message.getPassword() != null) {
					encodeString(message.getPassword(), data);
				}
			}
			message.setRemainLength(data.size());

			/* fixed header */
			out.write(encodeHeader(message));
			/* remain length */
			encodeRemainLength(message.getRemainLength(), out);
			/* data */
			data.writeTo(out);
			return ByteBuffer.wrap(out.toByteArray());
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (data != null) {
					data.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
