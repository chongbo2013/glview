package com.midcore.mqtt.client.callback;

public interface Callback<T> {
	public void onSuccess(T value);
	public void onFailure(Throwable throwable);
}
