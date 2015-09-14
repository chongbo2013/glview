package com.midcore.mqtt.client.callback;

import com.midcore.mqtt.core.message.Message;
import com.midcore.mqtt.core.message.PublishMessage;

public interface Listener {
	public void onConnected();
	public void onDisconnected();
	public void onPublish(PublishMessage publishMessage);
	public void onPingResp(Message message);
	public void onPingReq(Message message);
}
