package edu.utdallas.atomicbroadcastservice.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.nio.sctp.SctpChannel;


/**
 * @author prarabdh
 * Each node has an id, list of channels to other nodes, its own requests
 * Its current timestamp, a list of deferred messages  and a list of all received messages.
 */

public class Node {
	
	/**
	 * To uniquely identify the node. 
	 */
	
	private int nodeId;
	
	/**
	 * list of connections to other nodes
	 */
	private List<SctpChannel> sockets;
	
	/**
	 * A list of its own requests. This is needed while sending deferred messages.
	 * Only deferred messages with timestamp less than the next request are sent.
	 */
	private List<Request> ownRequests;
	
	/**
	 * Variable used to implement lamport's logical clock.
	 */
	private int currentTimeStamp;
	
	/**
	 * A list of deferred messages
	 */
	private List<DeferredMessage> deferedMessages;
	
	/**
	 * A list of received messages to be used to find out which message to deliver 
	 * when a DELIVER message is received.
	 * 
	 */
	private List<Message> receivedMessages;
	
	private int otherSendCount;
	
	private int mySendCount;
	
	private int deliverCount;
	
	private int terminateCount;
	
	private int maxNodes;
	
	public Node(int id) {
		nodeId = id;
		sockets = new ArrayList<SctpChannel>();
		currentTimeStamp = 0;
		deferedMessages = new CopyOnWriteArrayList<DeferredMessage>();
		ownRequests = new CopyOnWriteArrayList<Request>();
		receivedMessages = new CopyOnWriteArrayList<Message>();
	}
		
	public int getNodeId() {
		return nodeId;
	}
	
	public int getMaxNodes() {
		return maxNodes;
	}
	
	public void setMaxNodes(int maxNodes) {
		this.maxNodes = maxNodes;
	}
	public List<SctpChannel> getSockets() {
		return sockets;
	}

	public List<Request> getOwnRequests() {
		return ownRequests;
	}
	
	public List<DeferredMessage> getDeferredMessages() {
		return deferedMessages;
	}
	
	public List<Message> getReceivedMessages() {
		return receivedMessages;
	}
	
	public int getTimestamp() {
		return currentTimeStamp;
	}
	
	public void setTimestamp(int time) {
		currentTimeStamp = time;
	}
	

	public int getOtherSendCount() {
		return otherSendCount;
	}

	public void setOtherSendCount(int otherSendCount) {
		this.otherSendCount = otherSendCount;
	}

	public int getMySendCount() {
		return mySendCount;
	}

	public void setMySendCount(int mySendCount) {
		this.mySendCount = mySendCount;
	}
	
	public int getDeliverCount() {
		return deliverCount;
	}

	public void setDeliverCount(int deliverCount) {
		this.deliverCount = deliverCount;
	}
	
	public int getTerminateCount() {
		return terminateCount;
	}

	public void setTerminateCount(int terminateCount) {
		this.terminateCount = terminateCount;
	}

	/**
	 * @param m
	 * @return
	 * A method to check if a node has a request with a higher priority request than m.
	 */
	public boolean hasGreaterRequest(Message m) {
		if(ownRequests != null && !ownRequests.isEmpty()) {
			//if(ownRequests.peek().getMessage().getTimestamp() > m.getTimestamp())
			for(Request r: ownRequests){
				if(r.getMessage().getTimestamp() < m.getTimestamp())
					return true;
				else if(r.getMessage().getTimestamp() == m.getTimestamp()) {
					if(nodeId < m.getSenderNodeId())
						return true;
				}
			}	
		}
		return false;
	}
	
	public void addRequest(Request r) {
		ownRequests.add(r);
	}
	
	public void addDeferredMessage(Message m, SctpChannel socket) {
		deferedMessages.add(new DeferredMessage(m,socket));
	}
	
	public void addMessage(Message m) {
		receivedMessages.add(m);
	}

	public void setSockets(List<SctpChannel> sockets) {
		this.sockets = sockets;
		
	}
	
	public boolean allNodesDoneSending() {
		return getTerminateCount() == getMaxNodes();
	}
	
	public boolean allMessagesDelivered() {
		int totalSendCount = getOtherSendCount()+ getMySendCount(); 
		return totalSendCount == getDeliverCount();
	}

}