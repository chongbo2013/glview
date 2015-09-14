package com.midcore.mqtt.core;

import java.util.HashMap;
import java.util.Map;

public class MQTT {

	public final static String VERSION = "\00\06MQIsdp\03";

	public final static byte MESSAGE_TYPE_CONNECT = 0x01;
	public final static byte MESSAGE_TYPE_CONNACK = 0x02;
	public final static byte MESSAGE_TYPE_PUBLISH = 0x03;
	public final static byte MESSAGE_TYPE_PUBACK = 0x04;
	public final static byte MESSAGE_TYPE_PUBREC = 0x05;
	public final static byte MESSAGE_TYPE_PUBREL = 0x06;
	public final static byte MESSAGE_TYPE_PUBCOMP = 0x07;
	public final static byte MESSAGE_TYPE_SUBSCRIBE = 0x08;
	public final static byte MESSAGE_TYPE_SUBACK = 0x09;
	public final static byte MESSAGE_TYPE_UNSUBSCRIBE = 0x0a;
	public final static byte MESSAGE_TYPE_UNSUBACK = 0x0b;
	public final static byte MESSAGE_TYPE_PINGREQ = 0x0c;
	public final static byte MESSAGE_TYPE_PINGRESP = 0x0d;
	public final static byte MESSAGE_TYPE_DISCONNECT = 0x0e;
	
	public final static byte QOS_MOST_ONCE = 0;
	public final static byte QOS_LEAST_ONCE = 1;
	public final static byte QOS_ONCE = 2;

	public final static Map<Byte, String> TYPES = new HashMap<Byte, String>();
	static {
		TYPES.put((byte) MESSAGE_TYPE_CONNECT, "CONNECT");
		TYPES.put((byte) MESSAGE_TYPE_CONNACK, "CONNACK");
		TYPES.put((byte) MESSAGE_TYPE_PUBLISH, "PUBLISH");
		TYPES.put((byte) MESSAGE_TYPE_PUBACK, "PUBACK");
		TYPES.put((byte) MESSAGE_TYPE_PUBREC, "PUBREC");
		TYPES.put((byte) MESSAGE_TYPE_PUBREL, "PUBREL");
		TYPES.put((byte) MESSAGE_TYPE_PUBCOMP, "PUBCOMP");
		TYPES.put((byte) MESSAGE_TYPE_SUBSCRIBE, "SUBSCRIBE");
		TYPES.put((byte) MESSAGE_TYPE_SUBACK, "SUBACK");
		TYPES.put((byte) MESSAGE_TYPE_UNSUBSCRIBE, "UNSUBSCRIBE");
		TYPES.put((byte) MESSAGE_TYPE_UNSUBACK, "UNSUBACK");
		TYPES.put((byte) MESSAGE_TYPE_PINGREQ, "PINGREQ");
		TYPES.put((byte) MESSAGE_TYPE_PINGRESP, "PINGRESP");
		TYPES.put((byte) MESSAGE_TYPE_DISCONNECT, "DISCONNECT");
	}

}
