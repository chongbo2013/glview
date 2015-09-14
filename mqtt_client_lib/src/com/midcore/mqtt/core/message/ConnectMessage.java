package com.midcore.mqtt.core.message;

import com.midcore.mqtt.core.MQTT;

public class ConnectMessage extends Message {
	public ConnectMessage() {
		this.type = MQTT.MESSAGE_TYPE_CONNECT;
	}

	private boolean hasUsername;
	private boolean hasPassword;
	private boolean willRetain;
	private byte willQos;
	private boolean willFlag;
	private boolean cleanSession;

	private int keepAlive;

	private String clientID;
	
	private String willTopic;
	private String willMessage;

	private String username;
	private String password;
	
	private byte ack;

	public boolean isHasUsername() {
		return hasUsername;
	}

	public void setHasUsername(boolean hasUsername) {
		this.hasUsername = hasUsername;
	}

	public boolean isHasPassword() {
		return hasPassword;
	}

	public void setHasPassword(boolean hasPassword) {
		this.hasPassword = hasPassword;
	}

	public boolean isWillRetain() {
		return willRetain;
	}

	public void setWillRetain(boolean willRetain) {
		this.willRetain = willRetain;
	}

	public byte getWillQos() {
		return willQos;
	}

	public void setWillQos(byte willQos) {
		this.willQos = willQos;
	}

	public boolean isWillFlag() {
		return willFlag;
	}

	public void setWillFlag(boolean willFlag) {
		this.willFlag = willFlag;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public int getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public String getWillTopic() {
		return willTopic;
	}

	public void setWillTopic(String willTopic) {
		this.willTopic = willTopic;
	}

	public String getWillMessage() {
		return willMessage;
	}

	public void setWillMessage(String willMessage) {
		this.willMessage = willMessage;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public byte getAck() {
		return ack;
	}

	public void setAck(byte ack) {
		this.ack = ack;
	}
	
}
