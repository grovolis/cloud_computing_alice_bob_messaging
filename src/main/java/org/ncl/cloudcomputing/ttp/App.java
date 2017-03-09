package org.ncl.cloudcomputing.ttp;

import java.util.List;
import java.util.Map.Entry;

import org.ncl.cloudcomputing.common.AmazonBucket;
import org.ncl.cloudcomputing.common.AmazonQueue;

import com.amazonaws.services.sqs.model.Message;

public class App {

	private final static String ttpQueueName = "queue-ttp";
	private final static String aliceQueueName = "queue-alice";
	
	public static void main(String[] args) throws InterruptedException {
		
		AmazonBucket amazonBucket = new AmazonBucket();
		AmazonQueue amazonTTPQueue = new AmazonQueue(ttpQueueName);
		AmazonQueue amazonAliceQueue = new AmazonQueue(aliceQueueName);
		
		while (true) {
			List<Message> messages = amazonTTPQueue.receiveMessages();
					
			for (Message message : messages) {
				
				for (Entry<String, String> entry : message.getAttributes().entrySet()) {
			        System.out.println("    Name:  " + entry.getKey());
			        System.out.println("    Value: " + entry.getValue());
			    }
				
			}
			
			Thread.sleep(10000);
		}
	}

}
