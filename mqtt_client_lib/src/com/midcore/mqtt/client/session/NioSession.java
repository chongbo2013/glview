package com.midcore.mqtt.client.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.midcore.mqtt.client.Context;
import com.midcore.mqtt.client.message.MessageQueue;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.DisconnectMessage;
import com.midcore.mqtt.core.message.Message;

/**
 * 非阻塞式socket
 * @author wb_jing.lj
 */
public class NioSession extends Session implements ISession {

	private Selector selector = null;
	
	private MessageQueue messageQueue = new MessageQueue();
	
	public NioSession(Context context) {
		super(context);
	}
	
	@Override
	public void connect(String host, int port) throws IOException {
		if (!isClosed()) {
			close();
		}
		doConnect(host, port);
		selector = Selector.open();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);
		closed = false;
		new Thread(this).start();
	}
	
	@Override
	public void close() {
		super.close();
		if (selector != null && selector.isOpen()) {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		messageQueue.clear();
	}
	
	@Override
	public void sendMessage(Message message, boolean eagerly) {
		if (!isClosed()) {
			if (eagerly) {
				messageQueue.addFirst(message);
			} else {
				messageQueue.add(message);
			}
			try {
				socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void run() {
		try {
			while (!isClosed()) {
				if (selector.select(30) > 0) {
					doSelector();
				}
			}
		} catch (IOException e) {
			stopClient();
		}
	}
	
	private void doSelector() throws IOException {
		Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
		while(iter.hasNext()) {
			SelectionKey key = iter.next();
			if (!key.isValid()) {
				continue;
			}
			doKeys(key);
			iter.remove();
		}
	}
	
	private void doKeys(SelectionKey key) throws IOException {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			if (key.isReadable()) {
				readMessage(channel);
			}
			if (key.isWritable()) {
				sendMessage(channel);
			}
		} catch(Exception e){
			logger.warning("doKeys : "+ e.getMessage());
			key.cancel();
			throw new IOException(e.getMessage());
		}
	}
	
	private void sendMessage(SocketChannel channel) throws IOException {
		try {
			Message message = messageQueue.get();
			if (message != null) {
				/*logger.info("Send a message of type "
						+ message.getClass().getSimpleName());*/
				ByteBuffer buffer = context.getParser().encode(message);
				if (buffer != null) {
					channel.write(buffer);
				}
				if (message instanceof DisconnectMessage) {
					stopClient();
				} else if (messageQueue.size() == 0) {
					//register channel unwritable
					socketChannel.register(selector, SelectionKey.OP_READ);
				}
			}
		} catch (MQTTException e) {
			logger.warning("MQTTException" + e.getMessage());
			e.printStackTrace();
		}
	}
}
