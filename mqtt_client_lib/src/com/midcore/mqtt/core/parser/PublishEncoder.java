package com.midcore.mqtt.core.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PublishMessage;

public class PublishEncoder extends Encoder {

	@Override
	public ByteBuffer encode(Message msg) {
		ByteArrayOutputStream out = null;
		ByteArrayOutputStream data = null;
		try {
			out = new ByteArrayOutputStream();
			data = new ByteArrayOutputStream();
			PublishMessage message = (PublishMessage) msg;
			/* variable header */
			/* topic */
			encodeString(message.getTopic(), data);
			/* message id */
			if (message.getQos() == MQTT.QOS_LEAST_ONCE
					|| message.getQos() == MQTT.QOS_ONCE) {
				encodeMessageID(message.getMessageID(), data);
			}
			byte cmnsType = 0;
			cmnsType |= (message.getEncrypt() & 0xC0);
			if (message.getIsBuffer()) {
				cmnsType |= 0x20;
			}
			if (message.getIsCompress()) {
				cmnsType |= 0x10;
			}
			cmnsType |= (message.getReceiverType() & 0x0C);
			data.write(cmnsType);
			/* ReceiverID */
			if (message.getReceiverType() > 0) {
				encodeString(message.getReceiverID(), data);
			}
			
			
			/* payload */
			data.write(message.getPayload());
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
			throw new MQTTException(throwable);
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
	}

}
