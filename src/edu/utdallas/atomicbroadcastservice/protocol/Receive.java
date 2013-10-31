package edu.utdallas.atomicbroadcastservice.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

import edu.utdallas.atomicbroadcastservice.LogFormatter;
import edu.utdallas.atomicbroadcastservice.model.DeferredMessage;
import edu.utdallas.atomicbroadcastservice.model.Message;
import edu.utdallas.atomicbroadcastservice.model.MessageType;
import edu.utdallas.atomicbroadcastservice.model.Node;
import edu.utdallas.atomicbroadcastservice.model.Request;

/**
 * @author prarabdh
 * The main crux of the implementation. 
 * This class is responsible for receiving messages and replying with appropriate replies or deliver messages.
 * It implements a modified version of the Ricart and Agrawala protocol to handle multiple request at the same time from 
 * different or same process.  
 */
public class Receive implements Runnable {

	private Protocol protocol;
	
	private boolean end = false;
	
	private static Logger logger;
	
	public Receive(Protocol p) {
		this.protocol = p;
		logger = Logger.getLogger(Protocol.class.getName());
	}
	
	private String byteToString(ByteBuffer byteBuffer) {
		byteBuffer.position(0);
		byteBuffer.limit(512);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
    }
	
	@Override
	public void run() {
		int terminateCount = 0;
		System.out.println("Recieve thread Started.");
		List<SctpChannel> sockets = protocol.getNode().getSockets();
		while(!end) {
			for(SctpChannel socket: sockets) {
        		try {
        			socket.configureBlocking(false);
        			ByteBuffer byteBuffer;
        	        byteBuffer = ByteBuffer.allocate(512);
        	        MessageInfo messageInfo = socket.receive(byteBuffer,null,null);
        			if(messageInfo != null) {
        				String message = byteToString(byteBuffer);
        				Message m = Message.toMessage(message);
        				
        				Node n = protocol.getNode();
        				logger.info("Message Received: "+m);
        				n.setTimestamp(max(n.getTimestamp(), m.getTimestamp()) + 1);
        				MessageType type = m.getType();
        				switch(type) {
        				case REQUEST: 
        					//Add to message list
            				n.addMessage(m);
            				
        					if(n.hasGreaterRequest(m)) {
        						//add message to deferred message list;
        						n.addDeferredMessage(m,socket);
        						logger.info("Message added to deferred list: "+m);
        					}
        					else {
        						String id = m.getMessage()+"_"+m.getTimestamp();
        						Message reply = new Message(id, n.getTimestamp(),
        								n.getNodeId(), MessageType.REPLY);
        						//Send Message to sender Node Id
        						n.setTimestamp(n.getTimestamp() + 1);
        						protocol.sendMessage(socket, reply);
        					}
        					break;
        				case REPLY:
        					//We have received a reply message. Iterate over own request 
        					//to determine the recieveCount
        					//Assumed at least one request will match
        					Iterator<Request> iterator = n.getOwnRequests().iterator(); 
        					Request r = null;
        					while(iterator.hasNext()) {
        						r = iterator.next();
        						if(r.getMessage().isEqual(m)) {
        							r.setReceiveCount(r.getReceiveCount() + 1);
        							break;
        						}
        					}
        					if(r.getReceiveCount() == sockets.size()) {
        						// Send deliver message to each node
        						String id = r.getMessage().getMessage()+"_"+r.getMessage().getTimestamp();
        						
        						Message deliver = new Message(id, n.getTimestamp(),
        								n.getNodeId(), MessageType.DELIVER);
        						
        						n.setTimestamp(n.getTimestamp() + 1);
        						for(SctpChannel sock: sockets) {
        							protocol.sendMessage(sock, deliver);
        						}
        						//remove from own request
        						n.getOwnRequests().remove(r);
        						
        						//Deliver to own Application
        						//Notify the Application of the change in the queue.
        						n.setDeliverCount(n.getDeliverCount() + 1);
        						synchronized (protocol) {
        						
        							protocol.getDeliveryQueue().push(r.getMessage());
        							protocol.notifyAll();
								}
        						terminateCount = checkAndSendTerminate(
										terminateCount, sockets, n);
        					
        						//Send All Deferred Messages
        						logger.info("Sending Deferred Reply messages..");
        						if(!n.getDeferredMessages().isEmpty()) {
       								if(!n.getOwnRequests().isEmpty()) { 
        								// send deferred messages with timestamp less than my next request
    									for(DeferredMessage mess: n.getDeferredMessages()) {
    										if(n.getOwnRequests().get(0).getMessage().getTimestamp() > 
    											mess.getMessage().getTimestamp()) {
    											sendDeferredReply(n, mess);
    										}
    										// If the timestamp is same than compare on the basis of id of nodes 
    										else if(n.getOwnRequests().get(0).getMessage().getTimestamp() == 
    											mess.getMessage().getTimestamp()) {
    											if(mess.getMessage().getSenderNodeId() < n.getNodeId()) {
    												sendDeferredReply(n, mess);
    											}
    										}
    									}
    								}
        							// 	else if I have no request. Send all the deferred messages
        							else {
    									for(DeferredMessage mess: n.getDeferredMessages()) {
    										sendDeferredReply(n, mess);
    									}	
        							}
       							}
        					}
        					break;
        				case DELIVER:
        					//Deliver to Application        	
        					Message mess = null;
        					for(Message me: n.getReceivedMessages()) {
        						if(me.isEqual(m) && me.getSenderNodeId() == m.getSenderNodeId()) {
        							mess = me;
        							n.getReceivedMessages().remove(me);
        							break;
        						}
        					}
        					// Notify the Application of the change in the queue.
        					synchronized (protocol) {
        						protocol.getDeliveryQueue().push(mess);
        						protocol.notifyAll();
							}
        					
        					n.setDeliverCount(n.getDeliverCount() + 1);
        					terminateCount = checkAndSendTerminate(terminateCount, sockets, n);
        					break;
        				case TERMINATE:
        					
        					String str = m.getMessage();
        					if(str.equals("terminate")) {
        						// Increment terminate count
        						terminateCount++;
        						if(terminateCount == n.getMaxNodes()) {
        							// Application can begin testing
        							synchronized (protocol) {
        								protocol.setTest(true);
        								protocol.notifyAll();
									}
        							//end this thread
        							
        							end = true;
        						}
        					}
        					else {
        						int otherSendCount = n.getOtherSendCount();
        						otherSendCount += Integer.parseInt(str);
        						n.setOtherSendCount(otherSendCount);
        						n.setTerminateCount(n.getTerminateCount() + 1);
        						terminateCount = checkAndSendTerminate(
										terminateCount, sockets, n);
        					}
        					break;
        				}
        				
        			}
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        	}
        }
	}

	private synchronized int checkAndSendTerminate(int terminateCount,
			List<SctpChannel> sockets, Node n) throws IOException {
		if(n.allNodesDoneSending() && n.allMessagesDelivered()) {

			Message terminate = new Message("terminate", n.getTimestamp(), 
						n.getNodeId(), MessageType.TERMINATE);
			// Close Delivery log
			protocol.getDeliveryQueue().push(terminate);
			
			// Send a "terminate" message to all nodes
			n.setTimestamp(n.getTimestamp() + 1);
			for(SctpChannel sock: sockets) {
				protocol.sendMessage(sock, terminate);
			}
			terminateCount++;
			
		}
		return terminateCount;
	}
	
	private synchronized void sendDeferredReply(Node n, DeferredMessage mess)
			throws IOException {
		n.setTimestamp(n.getTimestamp() + 1);
		String id = mess.getMessage().getMessage()+ "_"+mess.getMessage().getTimestamp();
		Message reply = new Message(id, n.getTimestamp(),
				n.getNodeId(), MessageType.REPLY);
		protocol.sendMessage(mess.getSocket(), 
				reply);
		n.getDeferredMessages().remove(mess);
	}

	private int max(int timestamp, int timestamp2) {
		if(timestamp < timestamp2)
			return timestamp2;
		return timestamp;
	}
}
