package org.ncl.cloudcomputing.ttp;

import java.util.List;

import org.ncl.cloudcomputing.common.AmazonBucket;
import org.ncl.cloudcomputing.common.AmazonQueue;
import org.ncl.cloudcomputing.common.MessageStatus;

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
				
				String strMessageStatus = message.getAttributes().get("message-status").toString();
				Integer messageStatus = Integer.parseInt(strMessageStatus);
				
				if (messageStatus == MessageStatus.Alice_to_TTP.getValue()) {
					String sigAlice = message.getAttributes().get("sig-alice").toString();
					String docKey = message.getAttributes().get("doc-key").toString();
					
					
				}
				else if (messageStatus == MessageStatus.Bob_to_TTP.getValue()) {
					
				}
				
			}
			
			Thread.sleep(10000);
		}
	}

}
