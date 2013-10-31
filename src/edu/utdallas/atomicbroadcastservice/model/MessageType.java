package edu.utdallas.atomicbroadcastservice.model;

public enum MessageType {
	REQUEST("Request"),
	DELIVER("Deliver"),
	REPLY("Reply"),
	TERMINATE("Terminate");
	
	private String type;
	
	private MessageType(String type) {
		this.type = type;
	}
	
	public static MessageType parseMessageType(String typ) {
		typ = typ.trim();
		if(typ.equals("Request")) {
			return MessageType.REQUEST;
		}
		else if(typ.equals("Reply")) {
			return MessageType.REPLY;
		}
		else if(typ.equals("Deliver")) {
			return MessageType.DELIVER;
		}
		else if(typ.equals("Terminate")) {
			return MessageType.TERMINATE;
		}
		return MessageType.REQUEST;
			
	}
	
	public String toString() {
		return type.toString();
	}
}
