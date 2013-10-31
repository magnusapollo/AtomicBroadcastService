package edu.utdallas.atomicbroadcastservice.app;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.utdallas.atomicbroadcastservice.model.Node;
import edu.utdallas.atomicbroadcastservice.protocol.Protocol;

/**
 * 
 * @author prarabdh
 * 
 */
public class Application implements Runnable {
	
	private Protocol protocol;
	
	private List<Integer> stack;
	
	private int numberOfMessages;
	
	public Application(Protocol protocol, int numberOfMessages) {
		this.protocol = protocol;
		this.numberOfMessages = numberOfMessages;
		stack = new ArrayList<Integer>();
	}
	
	@Override
	public void run() {
		System.out.println("Application Started..");
		Deliver deliver = new Deliver(protocol, stack);
		Thread deliverThread = new Thread(deliver);
		deliverThread.start();
		
		try {
			synchronized (protocol) {
				
				while(!protocol.connectionEstablished()) {
					try {
						protocol.wait();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			Random r = new Random();
			for(int i = 0 ; i < numberOfMessages; i++) {
				int flag = r.nextInt(2);
				if(flag == 0) {
					insert(r.nextInt(10000));
				} else {
					delete();
				}
			}
			
			Node n = protocol.getNode();
			// send a terminate message to all nodes and increment its own terminate counter
			protocol.sendTerminate(n.getMySendCount()+ "");
			n.setTerminateCount(n.getTerminateCount() + 1);
			
			synchronized (protocol) {
				
				while(!protocol.canTest()) {
					try {
						protocol.wait();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			deliverThread.join();
			//Testing
			System.out.println("Testing..");
			if(comapreResults()) 
				System.out.println("Test Passed");
			else 
				System.out.println("Test Failed");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Testing: Each node compares its own file with the file of the first node.
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	private boolean comapreResults() throws IOException {
		File myfile = new File("Delivered_"+protocol.getNode().getNodeId()+".log");
		BufferedReader reader = new BufferedReader(new FileReader(myfile));
		if(!myfile.getName().equals("Delivered_0.log")) {
			BufferedReader reader2 = new BufferedReader(new FileReader("Delivered_0.log"));
			String line ="";
			while((line = reader2.readLine())!= null) {
				if(!line.equals(reader.readLine())) {
					return false;
				}
			}
		}	
		return true;
	}
	
	private void insert(int num) throws CharacterCodingException {
		broadcast("insert,"+num);
	}
	
	private void broadcast(String d) throws CharacterCodingException {
		protocol.broadcastMessage(d);
	}
	
	private void delete() throws CharacterCodingException {
		broadcast("delete");
	}
}
