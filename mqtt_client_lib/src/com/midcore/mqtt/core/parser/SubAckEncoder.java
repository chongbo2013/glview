package com.midcore.mqtt.core.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.SubAckMessage;

public class SubAckEncoder extends Encoder {

	@Override
	public ByteBuffer encode(Message msg) {
		ByteArrayOutputStream out = null;
		ByteArrayOutputStream data = null;
		try {
			out = new ByteArrayOutputStream();
			data = new ByteArrayOutputStream();
			SubAckMessage message = (SubAckMessage) msg;
			/* variable header */
			/* message id */
			encodeMessageID(message.getMessageID(), data);
			/* topics */
			List<Byte> qoss = message.getQoss();
			for (int i = 0; i < qoss.size(); i++) {
				byte qos = qoss.get(i);
				data.write(qos);
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
