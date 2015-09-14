package com.midcore.mqtt.client.message;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.midcore.mqtt.client.Config;
import com.midcore.mqtt.client.Context;
import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.message.ConnAckMessage;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PubAckMessage;
import com.midcore.mqtt.core.message.PubCompMessage;
import com.midcore.mqtt.core.message.PubRecMessage;
import com.midcore.mqtt.core.message.PubRelMessage;
import com.midcore.mqtt.core.message.PublishMessage;
import com.midcore.mqtt.core.message.SubAckMessage;
import com.midcore.mqtt.core.message.UnsubAckMessage;

public class MessageHandler {

	private static Logger logger = Logger.getLogger(Config.TAG);

	private Context context = null;

	public MessageHandler(Context context) {
		this.context = context;
	}

	/**
	 * handle message
	 * @param buffer
	 */
	public void handle(ByteBuffer buffer) {
		Message message = context.getParser().decode(buffer);
		if (message != null) {
			logger.info("Received a message of type "
					+ message.getClass().getSimpleName());
			switch (message.getType()) {
			case MQTT.MESSAGE_TYPE_CONNACK: // 0x02
				handleConnAck(message);
				break;
			case MQTT.MESSAGE_TYPE_PUBLISH: // 0x03
				handlePublish(message);
				break;
			case MQTT.MESSAGE_TYPE_PUBACK: // 0x04
				handlePubAck(message);
				break;
			case MQTT.MESSAGE_TYPE_PUBREC: // 0x05
				handlePubRec(message);
				break;
			case MQTT.MESSAGE_TYPE_PUBREL: // 0x06
				handlePubRel(message);
				break;
			case MQTT.MESSAGE_TYPE_PUBCOMP: // 0x07
				handlePubComp(message);
				break;
			case MQTT.MESSAGE_TYPE_SUBACK: // 0x09
				handleSubAck(message);
				break;
			case MQTT.MESSAGE_TYPE_UNSUBACK: //0x0b
				handleUnsubAck(message);
				break;
			case MQTT.MESSAGE_TYPE_PINGREQ: // 0x0c
				handlePingReq(message);
				break;
			case MQTT.MESSAGE_TYPE_PINGRESP: // 0x0d
				handlePingResp(message);
				break;
			case MQTT.MESSAGE_TYPE_DISCONNECT: // 0x0e
				handleDisconnect(message);
				break;			
			default:
				logger.warning("Handler not exist");
				break;
			}
		}
	}

	protected void handleConnAck(Message message) {
		context.getClient().connAckCallback((ConnAckMessage) message);
	}

	protected void handleSubAck(Message message) {
		SubAckMessage m = (SubAckMessage) message;
		context.getSender().sendQosAck(
				MQTT.TYPES.get(MQTT.MESSAGE_TYPE_SUBSCRIBE) + m.getMessageID());
		context.getSender().callback(m, null);
	}
	protected void handleUnsubAck(Message message) {
		UnsubAckMessage m = (UnsubAckMessage) message;
		context.getSender().sendQosAck(
				MQTT.TYPES.get(MQTT.MESSAGE_TYPE_UNSUBSCRIBE) + m.getMessageID());
		context.getSender().callback(m, null);
	}
	protected void handlePublish(Message message) {
		PublishMessage m = (PublishMessage) message;
		if (message.getQos() == MQTT.QOS_MOST_ONCE) {
			context.getClient().messageReceived(m);
		} else if (message.getQos() == MQTT.QOS_LEAST_ONCE) {
			context.getClient().messageReceived(m);
			PubAckMessage ack = new PubAckMessage();
			ack.setMessageID(m.getMessageID());
			context.getSender().send(ack);
		} else if (message.getQos() == MQTT.QOS_ONCE) {
			context.getMessageStore().qos2("" + m.getMessageID(), m);
			PubRecMessage rec = new PubRecMessage();
			rec.setMessageID(m.getMessageID());
			context.getSender().send(rec);
		}
	}

	protected void handlePubAck(Message message) {
		PubAckMessage m = (PubAckMessage) message;
		context.getSender().sendQosAck(
				MQTT.TYPES.get(MQTT.MESSAGE_TYPE_PUBLISH) + m.getMessageID());
		context.getSender().callback(m, null);
	}

	protected void handlePubRec(Message message) {
		PubRecMessage m = (PubRecMessage) message;
		context.getSender().sendQosAck(
				MQTT.TYPES.get(MQTT.MESSAGE_TYPE_PUBLISH) + m.getMessageID());

		PubRelMessage pubRelMessage = new PubRelMessage();
		pubRelMessage.setMessageID(m.getMessageID());
		context.getSender().send(pubRelMessage);
	}

	protected void handlePubRel(Message message) {
		PubRelMessage m = (PubRelMessage) message;
		context.getSender().sendQosAck(
				MQTT.TYPES.get(MQTT.MESSAGE_TYPE_PUBREC) + m.getMessageID());

		/* publish to Application */
		PublishMessage publishMessage = (PublishMessage) context
				.getMessageStore().getQos2("" + m.getMessageID());
		if (publishMessage != null) {
			context.getClient().messageReceived(publishMessage);
		}

		/* send comp message */
		PubCompMessage pubCompMessage = new PubCompMessage();
		pubCompMessage.setMessageID(m.getMessageID());
		context.getSender().send(pubCompMessage);
	}

	protected void handlePubComp(Message message) {
		PubCompMessage m = (PubCompMessage) message;
		context.getSender().sendQosAck(
				MQTT.TYPES.get(MQTT.MESSAGE_TYPE_PUBREL) + m.getMessageID());
		context.getSender().callback(m, null);
	}

	protected void handlePingResp(Message message) {
		context.getClient().pingRespCallback(message);
	}
	protected void handlePingReq(Message message) {
		context.getClient().pingReqCallback(message);
	}
	
	protected void handleDisconnect(Message message) {
		context.getClient().close();
	}

}
