package org.ncl.cloudcomputing.alice;

import java.util.HashMap;
import java.util.Map;

import org.ncl.cloudcomputing.common.AmazonBucket;
import org.ncl.cloudcomputing.common.AmazonQueue;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class App {
	
	private final static String fileName = "C:\\Users\\alper\\Desktop\\priority.png";
	
	private final static String ttpQueueName = "queue-ttp";
	private final static String aliceQueueName = "queue-alice";
	
	public static void main(String[] args) {
    	AmazonBucket amazonBucket = new AmazonBucket();
    	AmazonQueue amazonTTPQueue = new AmazonQueue(ttpQueueName);
    	AmazonQueue amazonAliceQueue = new AmazonQueue(aliceQueueName);
        
    	String docKey = amazonBucket.storeObject(fileName);
    	String sigAlice = "RYAN WILL DO"; // each exe must have different sigA at this point and they must produce their sigA
    	String hDoc = "RYAN WILL DO"; // each exe should hash the doc
    	
    	Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("String").withStringValue(sigAlice));
    	messageAttributes.put("hash-doc", new MessageAttributeValue().withDataType("String").withStringValue(hDoc));
    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringListValues(MessageStatus.Alice_to_TTP.getValue().toString()));
    	
    	SendMessageRequest request = new SendMessageRequest();
	    request.withMessageAttributes(messageAttributes);
	    request.setMessageBody("1");
	    amazonTTPQueue.sendMessage(request);
	}

}
