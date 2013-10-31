package edu.utdallas.atomicbroadcastservice.model;

import com.sun.nio.sctp.SctpChannel;

public class DeferredMessage {
	Message m;
	SctpChannel socket;
	
	public DeferredMessage(Message m, SctpChannel socket) {
		this.m = m;
		this.socket = socket;
	}

	public Message getMessage() {
		return m;
	}

	public SctpChannel getSocket() {
		return socket;
	}
}
