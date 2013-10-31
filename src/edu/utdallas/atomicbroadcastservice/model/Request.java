package edu.utdallas.atomicbroadcastservice.model;

public class Request {
	private Message m;
	private int recieveCount;
	
	public Request(Message m, int recieveCount) {
		this.m = m;
		this.recieveCount = recieveCount;
	}
	
	public Message getMessage() {
		return m;
	}
	public int getReceiveCount() {
		return recieveCount;
	}
	
	public void setReceiveCount(int reciveCount) {
		this.recieveCount = reciveCount;
	}
	
	
}
