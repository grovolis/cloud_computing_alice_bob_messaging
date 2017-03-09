package org.ncl.cloudcomputing.alice;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ncl.cloudcomputing.common.AWSBase;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Alice extends AWSBase implements Runnable {
	
	private String signature;
	
	private ArrayList<String> transactions;
	
	private Thread thread;
	
	public Alice() {
		this.transactions = new ArrayList<String>(); 
		this.signature = null;
	}
	
	// ryan will handle this procedure
	private String produceSignature() {
		return "";
	}
	
	public String putObjectToBucket(String filename) {
		File file = new File(filename);
		
		if(file.exists() && !file.isDirectory()) { 
		    throw new IllegalArgumentException("the file does not exist");
		}
		
		this.signature = this.produceSignature();
		
		return amazonBucket.storeObject(file);
	}
	
	public boolean sendMessageToTTP(String docKey) {
		try {
			if (this.signature == null) return false;
			
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
			
			String transactionId = UUID.randomUUID().toString();

	    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
	    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("String").withStringValue(this.signature));
	    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringListValues(MessageStatus.Alice_to_TTP.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("1");
		    this.amazonTTPQueue.sendMessage(request);
		    
		    transactions.add(transactionId);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void run() {
		
		while (true) {
			List<Message> messages = this.amazonAliceQueue.receiveMessages();
			for (Message message : messages) {
				
				String strMessageStatus = message.getAttributes().get("message-status").toString();
				Integer messageStatus = Integer.parseInt(strMessageStatus);
				
				if (messageStatus == MessageStatus.TTP_to_Alice.getValue()) {
					String transactionId = message.getAttributes().get("transaction-id").toString();
					String sigBob = message.getAttributes().get("sig-bob").toString();
					transactions.remove(transactionId);
				}
			}
			
			this.amazonAliceQueue.deleteMessages();
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void start() {
		if (thread == null) {
			thread = new Thread (this, "process messages");
			thread.start();
	    }
	}
}
