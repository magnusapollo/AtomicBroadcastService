package edu.utdallas.atomicbroadcastservice.model;

import java.util.ArrayList;
import java.util.List;


public class DeliverQueue {
	
	private List<Message> deliverQueue;
	
	public DeliverQueue() {
		deliverQueue = new ArrayList<Message>();
	}
	
	public void push(Message msg) {
		deliverQueue.add(msg);
	}
	
	public boolean isEmpty() {
		return deliverQueue.isEmpty();
	}
	
	public Message pop() throws InterruptedException {
		Message msg = deliverQueue.remove(0);
		return msg;
	}
}
