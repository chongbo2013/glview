package com.midcore.mqtt.client.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.midcore.mqtt.client.Context;
import com.midcore.mqtt.client.callback.Callback;
import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.MessageIDMessage;
import com.midcore.mqtt.core.message.PubRecMessage;
import com.midcore.mqtt.core.message.PublishMessage;

public class MessageSender {

	private static final long SCHEDULE_DELAY = 15 * 1000L;
	private static final int SCHEDULE_RETRY = 1;

	private Context context;

	private Map<String, ScheduledFuture<?>> scheduledFutures = Collections
			.synchronizedMap(new HashMap<String, ScheduledFuture<?>>());
	
	private Map<String, Callback<Message>> registedCallbacks = Collections
			.synchronizedMap(new HashMap<String, Callback<Message>>());
	

	public MessageSender(Context context) {
		this.context = context;
	}

	/**
	 * add to message queue
	 * @param message
	 */
	public void send(Message message) {
		if (message.getQos() == MQTT.QOS_LEAST_ONCE) {
			sendQos1((MessageIDMessage) message, 0);
		} else if (message.getQos() == MQTT.QOS_ONCE
				&& message instanceof PublishMessage) {
			sendQos2((PublishMessage) message, 0);
		} else {
			message.setQos(MQTT.QOS_MOST_ONCE);
			send0(message, false);
		}
	}
	
	/**
	 * add to the head of message queue
	 * @param message
	 */
	public void sendNow(Message message) {
		send0(message, true);
	}

	/**
	 * send with callbcak
	 * @param message
	 * @param callback
	 */
	public void send(Message message, Callback<Message> callback) {
		send(message);
		if (callback != null) {
			if (message.getQos() == MQTT.QOS_MOST_ONCE) {
				callback.onSuccess(message);
			} else {
				MessageIDMessage idMessage = (MessageIDMessage)message;
				String name = MQTT.TYPES.get(message.getType());
				registedCallbacks.put(name + idMessage.getMessageID(), callback);
			}
		}
	}

	/**
	 * qos1
	 * @param message
	 * @param tryTimes
	 */
	private void sendQos1(final MessageIDMessage message, int tryTimes) {
		String name = MQTT.TYPES.get(message.getType());
		final int t = tryTimes + 1;
		if (t > SCHEDULE_RETRY) {
			if (message instanceof PubRecMessage) {
				context.getMessageStore().getQos2("" + message.getMessageID());
			}
			scheduledFutures.remove(name + message.getMessageID());
			//execute callback
			callback(message, new MQTTException("no ack recieved"));
			return;
		}
		ScheduledFuture<?> future = context.getScheduler().schedule(
				new Runnable() {
					public void run() {
						message.setDup(true);
						sendQos1(message, t);
					}
				}, SCHEDULE_DELAY, TimeUnit.MILLISECONDS);
		scheduledFutures.put(name + message.getMessageID(), future);
		send0(message, false);
	}

	/**
	 * qos2
	 * @param message
	 * @param tryTimes
	 */
	private void sendQos2(final MessageIDMessage message, int tryTimes) {
		String name = MQTT.TYPES.get(message.getType());
		final int t = tryTimes + 1;
		if (t > SCHEDULE_RETRY) {
			context.getMessageStore().getQos2("" + message.getMessageID());
			scheduledFutures.remove(name + message.getMessageID());
			//execute callback
			callback(message, new MQTTException("no ack recieved"));
			return;
		}
		ScheduledFuture<?> future = context.getScheduler().schedule(
				new Runnable() {
					public void run() {
						message.setDup(true);
						sendQos2(message, t);
					}
				}, SCHEDULE_DELAY, TimeUnit.MILLISECONDS);
		scheduledFutures.put(name + message.getMessageID(), future);
		send0(message, false);
	}

	/**
	 * receive ack, cancel scheduler
	 * @param name
	 */
	public void sendQosAck(String name) {
		ScheduledFuture<?> future = scheduledFutures.remove(name);
		if (future != null) {
			future.cancel(false);
		}
	}
	
	public void callback(MessageIDMessage message, Throwable throwable) {
		String name;
		/* now we only have PUBLISH&SUBSCRIBE callback */
		if (message.getType() == MQTT.MESSAGE_TYPE_SUBSCRIBE || message.getType() == MQTT.MESSAGE_TYPE_SUBACK) {
			name = MQTT.TYPES.get(MQTT.MESSAGE_TYPE_SUBSCRIBE);
		} else if (message.getType() == MQTT.MESSAGE_TYPE_UNSUBSCRIBE || message.getType() == MQTT.MESSAGE_TYPE_UNSUBACK) {
			name = MQTT.TYPES.get(MQTT.MESSAGE_TYPE_UNSUBSCRIBE);
		} else if (message.getType() == MQTT.MESSAGE_TYPE_PUBLISH 
				|| message.getType() == MQTT.MESSAGE_TYPE_PUBACK 
				|| message.getType() == MQTT.MESSAGE_TYPE_PUBREC 
				|| message.getType() == MQTT.MESSAGE_TYPE_PUBREL
				|| message.getType() == MQTT.MESSAGE_TYPE_PUBCOMP) {
			name = MQTT.TYPES.get(MQTT.MESSAGE_TYPE_PUBLISH);
		} else {
			return;
		}
		Callback<Message> callback = registedCallbacks.remove(name + message.getMessageID());
		if (callback != null) {
			if (throwable != null) {
				callback.onFailure(throwable);
			} else {
				callback.onSuccess(message);
			}
		}
	}

	private void send0(Message message, boolean eagerly) {
		context.getSession().sendMessage(message, eagerly);
	}

}
