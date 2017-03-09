package org.ncl.cloudcomputing.bob;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ncl.cloudcomputing.common.AWSBase;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Bob extends AWSBase implements Runnable {

	private Thread thread;
	
	private String signature;
	
	private ArrayList<String> transactions;
	
	public Bob() {
		this.transactions = new ArrayList<String>(); 
		this.signature = null;
	}
	
	// ryan will handle this procedure
	private String produceSignature() {
		return "";
	}
	
	private boolean sendMessageToTTP(String transactionId) {
		try {
			if (this.signature == null) return false;
			
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

			messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("sig-bob", new MessageAttributeValue().withDataType("String").withStringValue(this.signature));
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringListValues(MessageStatus.Bob_to_TTP.getValue().toString()));
	    	
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
			List<Message> messages = this.amazonBobQueue.receiveMessages();
			for (Message message : messages) {
				
				String strMessageStatus = message.getAttributes().get("message-status").toString();
				Integer messageStatus = Integer.parseInt(strMessageStatus);
				
				if (messageStatus == MessageStatus.TTP_to_Bob.getValue()) {
					String transactionId = message.getAttributes().get("transaction-id").toString();
					String sigAlice = message.getAttributes().get("sig-alice").toString();
					
					this.signature = this.produceSignature();
					this.sendMessageToTTP(transactionId);
					transactions.add(transactionId);
				}
				else if (messageStatus == MessageStatus.TTP_to_Bob_doc.getValue()) {
					String transactionId = message.getAttributes().get("transaction-id").toString();
					String docKey = message.getAttributes().get("docKey").toString();
					
					S3Object object = this.amazonBucket.getObject(docKey);
					
					try {
						Files.copy(object.getObjectContent(), new File("/my/path/" + transactionId + "." + object.getObjectMetadata().getContentType()).toPath());
						object.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					transactions.remove(transactionId);
				}
			}
			
			this.amazonBobQueue.deleteMessages();
			
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
