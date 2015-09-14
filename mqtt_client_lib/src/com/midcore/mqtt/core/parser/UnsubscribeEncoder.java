package com.midcore.mqtt.core.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.UnsubscribeMessage;

public class UnsubscribeEncoder extends Encoder {

	@Override
	public ByteBuffer encode(Message msg) {
		ByteArrayOutputStream out = null;
		ByteArrayOutputStream data = null;
		try {
			out = new ByteArrayOutputStream();
			data = new ByteArrayOutputStream();
			UnsubscribeMessage message = (UnsubscribeMessage) msg;
			/* variable header */
			/* message id */
			encodeMessageID(message.getMessageID(), data);
			/* topics */
			List<String> topics = message.getTopics();
			for (int i = 0; i < topics.size(); i++) {
				String topic = topics.get(i);
				encodeString(topic, data);
			}

			message.setRemainLength(data.size());

			/* fixed header */
			out.write(encodeHeader(message));
			/* remain length */
			encodeRemainLength(message.getRemainLength(), out);
			/* data */
			out.write(data.toByteArray());
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
