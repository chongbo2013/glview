package com.midcore.mqtt.client;

import java.util.concurrent.ScheduledExecutorService;

import com.midcore.mqtt.client.message.MessageHandler;
import com.midcore.mqtt.client.message.MessageIDGenerator;
import com.midcore.mqtt.client.message.MessageSender;
import com.midcore.mqtt.client.message.MessageStore;
import com.midcore.mqtt.client.session.ISession;
import com.midcore.mqtt.client.session.Session;
import com.midcore.mqtt.core.parser.MQTTParser;

public class Context {
	private MQTTClient client;

	private ISession session = null;

	private MessageHandler messageHandler = null;

	private MQTTParser parser;

	private ScheduledExecutorService scheduler;

	private MessageSender sender = null;

	private MessageIDGenerator messageIDGenerator = null;

	private MessageStore messageStore = null;

	public Context(MQTTClient client) {
		this.client = client;
		sender = new MessageSender(this);
		messageHandler = new MessageHandler(this);
		session = new Session(this);
		messageIDGenerator = new MessageIDGenerator();
		messageStore = new MessageStore();
	}

	public int nextMessageID() {
		return messageIDGenerator.next();
	}

	public void registeParser(MQTTParser parser) {
		this.parser = parser;
	}

	public void registeScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	public ISession getSession() {
		return session;
	}

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	public MQTTParser getParser() {
		return parser;
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public MessageSender getSender() {
		return sender;
	}

	public MessageStore getMessageStore() {
		return messageStore;
	}

	public MQTTClient getClient() {
		return client;
	}

	public void stopClient() {
		if (!client.closed()) {
			client.close();
		} 
	}
	
	public void reconnetClient() {
		client.reconnect();
	}

	public void clear() {
		session.close();
		messageStore.clear();
		scheduler = null;
	}
}
