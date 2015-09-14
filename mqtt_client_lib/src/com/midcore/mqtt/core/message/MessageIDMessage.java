package com.midcore.mqtt.core.message;

/**
 * message with a message id, it's abstract
 */
public abstract class MessageIDMessage extends Message {

	private int messageID;

	public int getMessageID() {
		return messageID;
	}

	public void setMessageID(int messageID) {
		this.messageID = messageID;
	}

}
