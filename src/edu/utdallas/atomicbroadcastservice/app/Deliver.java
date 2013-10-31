package edu.utdallas.atomicbroadcastservice.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import edu.utdallas.atomicbroadcastservice.LogFormatter;
import edu.utdallas.atomicbroadcastservice.model.Message;
import edu.utdallas.atomicbroadcastservice.protocol.Protocol;

/**
 * @author prarabdh
 * Thread to poll the delivery queue to check for any delivered messaages.
 * It performs appropriate operations on the queue based on delivered message.
 */
public class Deliver implements Runnable {

	private Protocol protocol;
	
	private List<Integer> stack;
	
	private File file;
	
	private BufferedWriter writer;
	
	private static Logger logger;
	
	public Deliver(Protocol p, List<Integer> stack) {
		this.protocol = p;
		this.stack = stack;
		file = new File("Delivered_"+protocol.getNode().getNodeId()+".log");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			LogFormatter formatter = new LogFormatter();
			logger = Logger.getLogger(Application.class.getName());
			FileHandler handler;
			handler = new FileHandler("Application_"+protocol.getNode().getNodeId()+".log");
			handler.setFormatter(formatter);
			logger.addHandler(handler);
			logger.setUseParentHandlers(false);
			
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				//Blocking call to deliver message
				Message message = protocol.deliverMessage();
				if(message.getMessage().equals("terminate")) {
					System.out.println("Terminating Protocol..");
					writer.close();
					break;
				}
				System.out.println("Delivered: "+message);
				
				String[] instruction = message.getMessage().split(",");
				writer.write(message.getMessage());
				writer.write("\n");
				if(instruction.length == 1 && instruction[0].contains("delete")) {
					// delete from stack;
					if(!stack.isEmpty()) {
						stack.remove(0);
					}
					
				} else {
					// insert into stack;
					if(instruction[1].contains("_")) {
						int l = instruction[1].indexOf('_');
						instruction[1] = instruction[1].substring(0, l);
					}
					int num = Integer.parseInt(instruction[1]);
					stack.add(0,num);
				}
				//Print queue after each operation
				StringBuffer stringbuf = new StringBuffer();
				stringbuf.append("Stack: ");
				for(Integer i: stack) {
					stringbuf.append(i+" ");
				}
				stringbuf.append("\n");
				writer.write(stringbuf.toString());
				System.out.print(stringbuf.toString());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
