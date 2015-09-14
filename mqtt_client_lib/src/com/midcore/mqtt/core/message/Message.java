package com.midcore.mqtt.core.message;

/**
 * Created with IntelliJ IDEA. User: lijing Date: 13-1-6 Time: 下午9:39 To change
 * this template use File | Settings | File Templates.
 */
public abstract class Message {

	protected byte type;

	private boolean dup = false;

	private byte qos = 0;

	private boolean retain = false;

	private int remainLength = 0;

	public byte getType() {
		return type;
	}

	public boolean isDup() {
		return dup;
	}

	public void setDup(boolean dup) {
		this.dup = dup;
	}

	public byte getQos() {
		return qos;
	}

	public void setQos(byte qos) {
		this.qos = qos;
	}

	public boolean isRetain() {
		return retain;
	}

	public void setRetain(boolean retain) {
		this.retain = retain;
	}

	public int getRemainLength() {
		return remainLength;
	}

	public void setRemainLength(int remainLength) {
		this.remainLength = remainLength;
	}

}
