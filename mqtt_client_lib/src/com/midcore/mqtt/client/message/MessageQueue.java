package com.midcore.mqtt.client.message;

import java.util.LinkedList;

import com.midcore.mqtt.core.message.Message;

public class MessageQueue {

	private LinkedList<Message> messageQueue = new LinkedList<Message>();

	public MessageQueue() {
	}

	public boolean add(Message buffer) {
		synchronized (messageQueue) {
			return messageQueue.add(buffer);
		}
	}
	
	public boolean addFirst(Message buffer) {
		synchronized (messageQueue) {
			messageQueue.addFirst(buffer);
			return true;
		}
	}

	public Message get() {
		synchronized (messageQueue) {
			return messageQueue.poll();
		}
	}
	
	public int size() {
		synchronized (messageQueue) {
			return messageQueue.size();
		}
	}

	public void clear() {
		synchronized (messageQueue) {
			messageQueue.clear();
		}
	}

}
