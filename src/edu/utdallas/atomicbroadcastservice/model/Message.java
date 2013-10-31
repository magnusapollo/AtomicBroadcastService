package edu.utdallas.atomicbroadcastservice.model;

public class Message {
	private String message;
	private int timestamp;
	private int senderNodeId;
	private MessageType type;
	
	public MessageType getType() {
		return type;
	}

	public Message(String message, int timestamp, int senderNodeId, MessageType type) {
		this.message = message;
		this.timestamp = timestamp;
		this.senderNodeId = senderNodeId;
		this.type = type;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public int getTimestamp() {
		return this.timestamp;
	}
	
	public int getSenderNodeId() {
		return this.senderNodeId;
	}
	
	public String toString() {
		return new String(message+" "+timestamp+" "+senderNodeId+" "+type.toString());
	}
	
	public static Message toMessage(String message) {
		String[] s= message.split(" ");
		String mess = s[0];
		
		int timestamp = Integer.parseInt(s[1]);
		int senderNodeId = Integer.parseInt(s[2]);
		MessageType type = MessageType.parseMessageType(s[3]);
		Message m = new Message(mess, timestamp, senderNodeId, type);		
		return m;
	}
	
	
	public boolean isEqual(Message m) {
		
		String[] messageId = m.getMessage().split("_");
		int time = Integer.parseInt(messageId[1]);
		String str = messageId[0].trim();
		
		return (str.equals(this.message) && time == this.timestamp);		
	}
}