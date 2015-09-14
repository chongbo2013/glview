package com.midcore.mqtt.client.session;

import java.io.IOException;

import com.midcore.mqtt.core.message.Message;

public interface ISession {

	/**
	 * 建立socket连接
	 * @param host
	 * @param port
	 * @throws IOException
	 */
	public void connect(String host, int port) throws IOException;
	
	/**
	 * 关闭socket连接
	 */
	public void close();
	
	/**
	 * 发送消息
	 * @param message
	 * @param eagerly
	 */
	public void sendMessage(Message message, boolean eagerly);
	
}
