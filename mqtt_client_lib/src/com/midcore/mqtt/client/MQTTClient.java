package com.midcore.mqtt.client;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.midcore.mqtt.client.callback.Callback;
import com.midcore.mqtt.client.callback.Listener;
import com.midcore.mqtt.core.MQTT;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.ConnAckMessage;
import com.midcore.mqtt.core.message.ConnectMessage;
import com.midcore.mqtt.core.message.DisconnectMessage;
import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PingReqMessage;
import com.midcore.mqtt.core.message.PublishMessage;
import com.midcore.mqtt.core.message.SubscribeMessage;
import com.midcore.mqtt.core.message.UnsubscribeMessage;
import com.midcore.mqtt.core.parser.ConnAckDecoder;
import com.midcore.mqtt.core.parser.ConnectEncoder;
import com.midcore.mqtt.core.parser.DisconnectDecoder;
import com.midcore.mqtt.core.parser.DisconnectEncoder;
import com.midcore.mqtt.core.parser.MQTTParser;
import com.midcore.mqtt.core.parser.PingReqDecoder;
import com.midcore.mqtt.core.parser.PingReqEncoder;
import com.midcore.mqtt.core.parser.PingRespDecoder;
import com.midcore.mqtt.core.parser.PingRespEncoder;
import com.midcore.mqtt.core.parser.PubAckDecoder;
import com.midcore.mqtt.core.parser.PubAckEncoder;
import com.midcore.mqtt.core.parser.PubCompDecoder;
import com.midcore.mqtt.core.parser.PubCompEncoder;
import com.midcore.mqtt.core.parser.PubRecDecoder;
import com.midcore.mqtt.core.parser.PubRecEncoder;
import com.midcore.mqtt.core.parser.PubRelDecoder;
import com.midcore.mqtt.core.parser.PubRelEncoder;
import com.midcore.mqtt.core.parser.PublishDecoder;
import com.midcore.mqtt.core.parser.PublishEncoder;
import com.midcore.mqtt.core.parser.SubAckDecoder;
import com.midcore.mqtt.core.parser.SubscribeEncoder;
import com.midcore.mqtt.core.parser.UnsubAckDecoder;
import com.midcore.mqtt.core.parser.UnsubscribeEncoder;

public class MQTTClient {

	private static Logger logger = Logger.getLogger(Config.TAG);

	/* CONNECT_TIMEOUT */
	private static final long CONNECT_TIMEOUT = 30 * 1000L;
	private static final long RECONNECT_TIMES = 3;
	
	/* KEEPALIVE_SECS */
	private static final int KEEPALIVE_SECS = 6;

	private String host = "0.0.0.0";
	private int port = 1883;
	private String clientID;
	private String username;
	private String password;
	private int keepAlive = KEEPALIVE_SECS;
	private boolean closed = true;

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}

	public void setHost(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	private MQTTParser parser = null;

	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> heartbeatHandler;
	private ScheduledFuture<?> connectHandler;
	private ScheduledFuture<?> reconnectHandler;
	private Callback<ConnAckMessage> connectCallback;
	
	private Listener listener = null;

	private Context context = null;

	public MQTTClient() {
		context = new Context(this);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void init() {
		parser = new MQTTParser();
		parser.registeEncoder(new ConnectEncoder());
		parser.registeEncoder(new PingReqEncoder());
		parser.registeEncoder(new PingRespEncoder());
		parser.registeEncoder(new SubscribeEncoder());
		parser.registeEncoder(new PublishEncoder());
		parser.registeEncoder(new PubAckEncoder());
		parser.registeEncoder(new PubRecEncoder());
		parser.registeEncoder(new PubRelEncoder());
		parser.registeEncoder(new PubCompEncoder());
		parser.registeEncoder(new DisconnectEncoder());
		parser.registeEncoder(new UnsubscribeEncoder());

		parser.registeDecoder(new ConnAckDecoder());
		parser.registeDecoder(new PingRespDecoder());
		parser.registeDecoder(new PingReqDecoder());
		parser.registeDecoder(new SubAckDecoder());
		parser.registeDecoder(new PublishDecoder());
		parser.registeDecoder(new PubAckDecoder());
		parser.registeDecoder(new PubRecDecoder());
		parser.registeDecoder(new PubRelDecoder());
		parser.registeDecoder(new PubCompDecoder());
		parser.registeDecoder(new DisconnectDecoder());
		parser.registeDecoder(new UnsubAckDecoder());

		context.registeParser(parser);
		scheduler = Executors.newScheduledThreadPool(1);
		context.registeScheduler(scheduler);
	}

	public synchronized void connect(Callback<ConnAckMessage> connectCallback) {
		connect(true, connectCallback);
	}

	public synchronized void connect(boolean cleanSession, Callback<ConnAckMessage> connectCallback) {
		//clear and init
		clear();
		init();
		this.connectCallback = connectCallback;
		try {
			socketConnet();
		} catch (IOException e) {
			this.connectCallback.onFailure(e);
			return;
		}
		
		connectHandler = scheduler.schedule(new Runnable() {
			public void run() {
				/* Connection timeout (no CONNACK) */
				MQTTClient.this.connectCallback.onFailure(new MQTTException("Connection timeout (no CONNACK)"));
			}
		}, CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
		
		/* add to the head of queue */
		addSendMessage0(buildConnectMessage(cleanSession), null);
		
	}
	
	private void socketConnet() throws IOException {
		try {
			context.getSession().connect(host, port);
			logger.info("Connected to " + host + " and port:" + port);
		} catch (SocketTimeoutException s){
			logger.warning("Connecting fail or time out : SocketTimeoutException " + s.getMessage());
			throw s;
		} catch (IOException e) {
			logger.warning("Connecting fail or time out : IOException " + e.getMessage());
			throw e;
		}
	}
	
	private ConnectMessage buildConnectMessage(boolean cleanSession) {
		ConnectMessage message = new ConnectMessage();
		message.setClientID(clientID);
		if (this.username != null) {
			message.setHasUsername(true);
			message.setUsername(this.username);
		}
		if (this.password != null) {
			message.setHasPassword(true);
			message.setPassword(password);
		}
		message.setKeepAlive(keepAlive);
		message.setCleanSession(cleanSession);
		return message;
	}

	public void disconnect() {
		if (closed()) {
			return;
		}
		closed = true;
		addSendMessage0(new DisconnectMessage(), null);
		if (this.listener != null) {
			this.listener.onDisconnected();
		}
	}

	/**
	 * close session
	 */
	public synchronized void close() {
		context.clear();
		context.getSession().close();
		if (!closed() && this.listener != null) {
			this.listener.onDisconnected();
		}
		closed = true;
	}
	
	public synchronized void clear() {
		context.getSession().close();
		if (heartbeatHandler != null) {
			heartbeatHandler.cancel(false);
			heartbeatHandler = null;
		}
		if (reconnectHandler != null) {
			reconnectHandler.cancel(false);
			reconnectHandler = null;
		}
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			scheduler = null;
		}
		context.clear();
	}
	
	/**
	 * ConnAck
	 * @param message
	 */
	public void connAckCallback(ConnAckMessage message) {
		//logger.info("connAckCallback invoked, ackCode=" + message.getAck());
		
		/* it's a reconnect ack */
		if (this.reconnectHandler != null) {
			this.reconnectHandler.cancel(false);
			this.reconnectHandler = null;
			if (message.getAck() == 0) {
				this.heartbeat();
			} else {
				close();
			}
			return;
		}
		
		/* it's the first connect ack */
		if (this.connectHandler.isDone()) {
			return;
		}
		this.connectHandler.cancel(false);
		
		if (message.getAck() != 0) {
//			String errMsg = null;
//			switch (message.getAck()) {
//			case 1:
//				errMsg = "Unacceptable protocol version";
//				break;
//			case 2:
//				errMsg = "Identifier rejected";
//				break;
//			case 3:
//				errMsg = "Server unavailable";
//				break;
//			case 4:
//				errMsg = "Bad username or password";
//				break;
//			case 5:
//				errMsg = "Not authorized";
//				break;
//			default:
//				break;
//			}
//			this.connectCallback.onFailure(new MQTTException(errMsg));
//			return;
		} else {
			closed = false;
			if (this.listener != null) {
				this.listener.onConnected();
			}
			this.heartbeat();
		}
		this.connectCallback.onSuccess(message);
	}
	
	public void pingRespCallback(Message message) {
		if (this.listener != null) {
			this.listener.onPingResp(message);
		}
	}
	public void pingReqCallback(Message message) {
		if (this.listener != null) {
			this.listener.onPingReq(message);
		}
	}

	public void subscribe(String topic) {
		subscribe(topic, MQTT.QOS_MOST_ONCE);
	}

	public void subscribe(String topic, byte qos) {
		subscribe(topic, qos, null);
	}

	/**
	 * subscribe a topic
	 * @param topic
	 * @param qos
	 * @param callback
	 */
	public void subscribe(String topic, byte qos, Callback<Message> callback) {
		if (closed()) {
			if (callback != null) {
				callback.onFailure(new MQTTException("Session closed"));
			}
			return;
		}
		SubscribeMessage message = new SubscribeMessage();
		message.setMessageID(context.nextMessageID());
		message.addTopic(topic, qos);
		addSendMessage(message, callback);
		this.heartbeat();
	}
	
	/**
	 * unsubscribe a topic
	 * @param topic
	 */
	public void unsubscribe(String topic) {
		unsubscribe(topic, null);
	}
	
	/**
	 * unsubscribe a topic
	 * @param topic
	 * @param callback
	 */
	public void unsubscribe(String topic, Callback<Message> callback) {
		if (closed()) {
			if (callback != null) {
				callback.onFailure(new MQTTException("Session closed"));
			}
			return;
		}
		UnsubscribeMessage message = new UnsubscribeMessage();
		message.setMessageID(context.nextMessageID());
		message.addTopic(topic);
		addSendMessage(message, callback);
		this.heartbeat();
	}

	public void publish(String topic, byte[] payload) {
		publish(topic, payload, MQTT.QOS_MOST_ONCE);
	}

	public void publish(String topic, byte[] payload, byte qos) {
		publish(topic, payload, qos, false);
	}

	public void publish(String topic, byte[] payload, byte qos, boolean retain) {
		publish(topic, payload, null, qos, retain, null);
	}

	/**
	 * publish a message
	 * @param topic
	 * @param payload
	 * @param qos
	 * @param retain
	 * @param callback
	 */
	public void publish(String topic, byte[] payload, JSONObject obj ,byte qos, boolean retain,
			Callback<Message> callback) {
		PublishMessage publishMessage = new PublishMessage();
		publishMessage.setQos(qos);
		publishMessage.setRetain(retain);
		publishMessage.setTopic(topic);
		publishMessage.setPayload(payload);
		publish(publishMessage, callback);
	}
	
	public void publish(PublishMessage publishMessage, Callback<Message> callback) {
		if (closed()) {
			if (callback != null) {
				callback.onFailure(new MQTTException("Session closed"));
			}
			return;
		}
		if (publishMessage.getQos() != MQTT.QOS_MOST_ONCE) {
			publishMessage.setMessageID(context.nextMessageID());
		}
		addSendMessage(publishMessage, callback);
		this.heartbeat();
	}
	
	/**
	 * receive publish message
	 * @param publishMessage
	 */
	public void messageReceived(PublishMessage publishMessage) {
		logger.info("Received a publish message : messageID="
				+ publishMessage.getMessageID() + "\nqos="
				+ publishMessage.getQos() + "\ntopic="
				+ publishMessage.getTopic());
		if (this.listener != null) {
			this.listener.onPublish(publishMessage);
		}
	}

	final Runnable pingreqDeamon = new Runnable() {
		public void run() {
			addSendMessage(new PingReqMessage(), null);
		}
	};

	/**
	 * heartbeat/send a PingReq message
	 */
	public void heartbeat() {
		/*if (heartbeatHandler != null) {
			heartbeatHandler.cancel(false);
		}
		heartbeatHandler = scheduler.scheduleWithFixedDelay(pingreqDeamon,
				keepAlive, keepAlive, TimeUnit.SECONDS);*/
	}

	public void addSendMessage(Message message, Callback<Message> callback) {
		if (closed()) {
			return;
		}
		addSendMessage0(message, callback);
	}
	private void addSendMessage0(Message message, Callback<Message> callback) {
		// NetworkOnMainThreadException
		try {
			context.getSender().send(message, callback);
		} catch (Exception e) {
			if (callback != null) {
				callback.onFailure(e);
			}
		}
	}

	public Context getContext() {
		return context;
	}
	
	public synchronized boolean closed() {
		return closed;
	}
	
	/**
	 * reconnect to mqtt server
	 */
	public void reconnect() {
		reconnect(1);
	}
	
	private void reconnect(final int attempt) {
		if (closed()) {
			return;
		}
		
		if (attempt > RECONNECT_TIMES) {
			this.reconnectHandler = null;
			logger.warning("Reconnect attempt too muth, close");
			close();
			return;
		}
		logger.info("Reconnect attempt " + attempt);
		
		try {
			context.getSession().close();
			socketConnet();
			context.getSender().sendNow(buildConnectMessage(false));
		} catch (IOException e) {
		}
		
		try {
			this.reconnectHandler = scheduler.schedule(new Runnable() {
				public void run() {
					reconnect(attempt + 1);
				}
			}, CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
		}
	}
}
