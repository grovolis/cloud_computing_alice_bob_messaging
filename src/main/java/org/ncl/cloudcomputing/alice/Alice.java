package org.ncl.cloudcomputing.alice;

import java.util.HashMap;
import java.util.Map;

import org.ncl.cloudcomputing.common.AmazonBucket;
import org.ncl.cloudcomputing.common.AmazonQueue;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Alice {

	private final static String ttpQueueName = "queue-ttp";
	private final static String aliceQueueName = "queue-alice";
	
	private AmazonBucket amazonBucket;
	private AmazonQueue amazonTTPQueue;
	private AmazonQueue amazonAliceQueue;
	
	private String signature;
	
	public Alice() {
		this.amazonBucket = new AmazonBucket();
		this.amazonTTPQueue = new AmazonQueue(ttpQueueName);
		this.amazonAliceQueue = new AmazonQueue(aliceQueueName);
		
		this.signature = ""; // ryan will do
	}
	
	public String putObjectToBucket(String filename) {
		return amazonBucket.storeObject(filename);
	}
	
	public boolean sendMessageToTTP(String docKey) {
		try {
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

	    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
	    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("String").withStringValue(this.signature));
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringListValues(MessageStatus.Alice_to_TTP.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("1");
		    amazonTTPQueue.sendMessage(request);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
