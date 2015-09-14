package com.midcore.mqtt.client.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import com.midcore.mqtt.client.Config;
import com.midcore.mqtt.client.Context;
import com.midcore.mqtt.core.MQTTException;
import com.midcore.mqtt.core.message.Message;

/**
 * 阻塞式socket
 * @author wb_jing.lj
 */
public class Session implements Runnable, ISession {

	protected static Logger logger = Logger.getLogger(Config.TAG);
	
	protected ByteBuffer buffer = null;
	protected byte[] array = new byte[1024];
	
	protected SocketChannel socketChannel = null;
	protected boolean closed = true;
	
	protected Context context = null;
	
	public Session(Context context) {
		super();
		this.context = context;
	}

	@Override
	public void connect(String host, int port) throws IOException {
		if (!isClosed()) {
			close();
		}
		doConnect(host, port);
		closed = false;
		new Thread(this).start();
	}
	
	protected void doConnect(String host, int port) throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(host, port);
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(true);
		socketChannel.socket().setTcpNoDelay(true);
		socketChannel.socket().connect(socketAddress, 5000);
	}

	@Override
	public void close() {
		closed = true;
		if (socketChannel != null && socketChannel.isConnected()) {
			try {
				socketChannel.close();
				logger.info("Socket disconnect");
			} catch (IOException e) {
				logger.warning("Channel closed to failed: IOException");
			}
		}
	}
	
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void sendMessage(Message message, boolean eagerly) {
		if (!isClosed()) {
			ByteBuffer buffer = context.getParser().encode(message);
			if (buffer != null) {
				try {
					socketChannel.write(buffer);
				} catch (Exception e) {
					logger.warning("Exception : "+ e.getMessage());
				}
			}
		}
	}
	
	protected void readMessage(SocketChannel channel) throws IOException {
		/* to be optimized */
		ByteBuffer byteBuffer = null;
		if (this.buffer != null && this.buffer.remaining() > 0) {
			int remaining = this.buffer.remaining();
			int bufferSize = 0;
			if (remaining > 1024) {
				bufferSize = remaining << 1;
			} else {
				bufferSize = remaining + 1024;
			}
			byteBuffer = ByteBuffer.allocate(bufferSize);
			byteBuffer.put(this.buffer.array());
		} else {
			byteBuffer = ByteBuffer.wrap(array);
		}
		int count = channel.read(byteBuffer);
		if (count > 0) {
			byteBuffer.flip();
			try {
				for (;;) {
					if (context.getParser().decodable(byteBuffer)) { /* ok */
						context.getMessageHandler().handle(byteBuffer);
					} else { /* need data */
						logger.warning("data not ready, waiting");
						break;
					}
					if (byteBuffer.remaining() < 2) {
						break;
					}
				}
				if (byteBuffer.remaining() <= 0) {
					this.buffer = null;
				} else {
					byte[] buf = new byte[byteBuffer.remaining()];
					byteBuffer.get(buf);
					this.buffer = ByteBuffer.wrap(buf);
				}
			} catch (MQTTException e) {
				logger.warning("error : " + e.getMessage());
				e.printStackTrace();
				this.buffer = null;
			}
		} else if (count < 0) {
			throw new IOException("Connection error");
		}
	}
	
	@Override
	public void run() {
		try {
			while (!isClosed()) {
				readMessage(socketChannel);
			}
		} catch (Exception e) {
			logger.warning("Exception : "+ e.getMessage());
			stopClient();
			close();
		}
	}
	
	public void stopClient() {
		if (!closed) {
			context.stopClient();
		}
	}
	
}
