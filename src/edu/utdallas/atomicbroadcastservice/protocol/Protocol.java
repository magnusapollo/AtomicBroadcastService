package edu.utdallas.atomicbroadcastservice.protocol;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

import edu.utdallas.atomicbroadcastservice.LogFormatter;
import edu.utdallas.atomicbroadcastservice.model.DeliverQueue;
import edu.utdallas.atomicbroadcastservice.model.Message;
import edu.utdallas.atomicbroadcastservice.model.MessageType;
import edu.utdallas.atomicbroadcastservice.model.Node;
import edu.utdallas.atomicbroadcastservice.model.Request;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author prarabdh
 * Main protocol class. Establishes connections and spawns a receive thread.
 */
public class Protocol implements Runnable {

    private Node node;
	
	private String configFilePath;
	
	private int serverPort;
	
	private boolean hasConnectionEstablished;
	
	private DeliverQueue deliverQueue;
	
	private static Logger logger;
	/**
	 * @param id
	 * @param deliverQueue
	 * @param configFilePath
	 */
	public Protocol(int id, DeliverQueue deliverQueue, String configFilePath) {
    	node = new Node(id);
    	hasConnectionEstablished = false;
    	this.deliverQueue = deliverQueue;
    	this.configFilePath = configFilePath;
    	LogFormatter formatter = new LogFormatter();
		logger = Logger.getLogger(Protocol.class.getName());
		FileHandler file;
		try {
			file = new FileHandler("Protocol_"+node.getNodeId()+".log");
			file.setFormatter(formatter);
			logger.addHandler(file);
			logger.setUseParentHandlers(false);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
    }
    
    public synchronized boolean connectionEstablished() {
    	return hasConnectionEstablished;
    }
    
    public void sendMessage(SctpChannel socket,
			Message message) throws IOException {
    	MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			
    	ByteBuffer sendBuffer = ByteBuffer.allocate(512);
		sendBuffer.clear();
		sendBuffer.put(message.toString().getBytes());
		sendBuffer.flip();
		socket.send(sendBuffer, messageInfo);
		
		logger.info("Message sent: "+message);
	}
    
    public void sendTerminate(String message) {
    	List<SctpChannel> sockets = node.getSockets();
    	
		try {
   			node.setTimestamp(node.getTimestamp() + 1);
   			Message m = new Message(message, node.getTimestamp(), node.getNodeId(), MessageType.TERMINATE);
   			for(SctpChannel clientSock : sockets) {
   				sendMessage(clientSock, m);
   			}
   			
   		} catch (IOException ex) {
   			Logger.getLogger(Protocol.class.getName()).log(Level.SEVERE, null, ex);
   		}
    }
    
    public void broadcastMessage(String message) throws CharacterCodingException {
		List<SctpChannel> sockets = node.getSockets();
    	//Add request to own requests and broadcast
		
		try {
   			node.setTimestamp(node.getTimestamp() + 1);
   			node.setMySendCount(node.getMySendCount() + 1);
   			Message m = new Message(message, node.getTimestamp(), node.getNodeId(), MessageType.REQUEST);
   			Request r = new Request(m, 0);
   			node.addRequest(r);
   			if(sockets == null) {
   				System.out.println("No other threads to send data..");
   			}
   			else {
   				for(SctpChannel clientSock : sockets) {
   					sendMessage(clientSock, m);
   				}
   			}
   		} catch (IOException ex) {
   			logger.log(Level.SEVERE, null, ex);
   		}
   	}
    
    public synchronized Message deliverMessage() throws InterruptedException {
    	while(deliverQueue.isEmpty()) {
    		wait();
    	}
    	return deliverQueue.pop();
    }
    
    public Node getNode() {
    	return this.node;
    }
    
    public DeliverQueue getDeliveryQueue() {
    	return this.deliverQueue;
    }
    
    /**
     * Establishes connections between nodes.Every node connects to servers running before it.
     * It then starts a server of its own and waits for connections from clients.
     * @return list of connections 
     */
    
	public void connect() {
    	File file = new File(configFilePath);
    	BufferedReader br;
    	SctpServerChannel serverSock;
    	List<SctpChannel> sockets = node.getSockets();
    	try {
    		synchronized (this) {
    			InetSocketAddress serverAddr;
    			br = new BufferedReader(new FileReader(file));
    			String line = null;
    			int maxNodes = Integer.parseInt(br.readLine());
    			node.setMaxNodes(maxNodes);
    			String address ="";
    			int port = 0;
    			while ((line = br.readLine()) != null) {
    				SctpChannel socket;
    				String[] idAddressPort = line.split(" ");
    				int id = Integer.parseInt(idAddressPort[0]);
    				address = idAddressPort[1];
    				port = Integer.parseInt(idAddressPort[2]);
    				if(id == node.getNodeId()) {
    					serverPort = port;
    					break;
    				}
    				serverAddr = new InetSocketAddress(address, port);
    				socket = SctpChannel.open();
    				socket.connect(serverAddr, 0, 0);
    				sockets.add(socket);
    			}
    			// Start server and wait for connections
    			serverSock = SctpServerChannel.open();
    			serverAddr = new InetSocketAddress(serverPort);
    			serverSock.bind(serverAddr);
    			System.out.println("Service started at "+serverPort+" port.");
    			int count = node.getNodeId() + 1;
    			while(count < maxNodes) {
    				SctpChannel clientSock;
    				clientSock = serverSock.accept();
    				count++;
    				sockets.add(clientSock);
    			}
    			
    			System.out.println("All Connections established..");
    			hasConnectionEstablished = true;
				notifyAll();
				br.close();

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	
    }
    private boolean test;
    
    public void setTest(boolean value) {
    	this.test = true;
    }
    
    public boolean canTest() {
    	return test;
    }
    
    private boolean closeDeliveryLog;
    
    public boolean getCloseDeliveryLog() {
    	return closeDeliveryLog;
    }
    
    public void setCloseDeliveryLog(boolean value) {
    	this.closeDeliveryLog = value;
    }
    
    public void run() {
    	connect();
    	Thread receive = new Thread(new Receive(this));
        receive.start();
    }
}
