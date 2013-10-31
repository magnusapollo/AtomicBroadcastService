package edu.utdallas.atomicbroadcastservice;

import edu.utdallas.atomicbroadcastservice.app.Application;
import edu.utdallas.atomicbroadcastservice.model.DeliverQueue;
import edu.utdallas.atomicbroadcastservice.protocol.Protocol;

public class AtomicBroadcastServiceDemo {
	public static void main(String[] args) {
		try {
			if(args.length < 3) {
				System.out.println("Usage: java "
						+ "edu.utdallas.atomicbroadcastservice.AtomicBroadcastServiceDemo "
						+ "<Node Id> <Number of messages> <Configuration File Path>");
				System.exit(-1);
			}
			DeliverQueue deliverQueue = new DeliverQueue();
			String configFilePath = args[2];
			Protocol protocol = new Protocol(Integer.parseInt(args[0]), deliverQueue, configFilePath);
			Thread protocolThread = new Thread(protocol);
			protocolThread.start();
			Thread application = new Thread(new Application(protocol, Integer.parseInt(args[1])));
			application.start();
			application.join();
			System.out.println("Log files(Application, Protocol, Delivery) are generated at: "+System.getProperty("user.dir"));
		} catch (NumberFormatException | InterruptedException e) {
			e.printStackTrace();
		} 
	}
}
