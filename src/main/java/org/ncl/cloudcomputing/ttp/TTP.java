package org.ncl.cloudcomputing.ttp;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ncl.cloudcomputing.common.AWSBase;
import org.ncl.cloudcomputing.common.Database;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class TTP extends AWSBase implements Runnable {
	
	private Thread thread;
	
	private Database database;
	
	public TTP() {
		this.database = new Database();
	}
	
	// changed this to update it for the byte[]
	private void sendMessageToBob(String transactionId, byte[] sigAlice) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
		
		// changed this to update it for the byte[]
    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(sigAlice)));
    	
    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringListValues(MessageStatus.TTP_to_Bob.getValue().toString()));
    	
    	SendMessageRequest request = new SendMessageRequest();
	    request.withMessageAttributes(messageAttributes);
	    request.setMessageBody("1");
	    this.amazonBobQueue.sendMessage(request);
	}
	
	private void sendDocumentKeyToBob(String transactionId, String docKey) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringListValues(MessageStatus.TTP_to_Bob_doc.getValue().toString()));
    	
    	SendMessageRequest request = new SendMessageRequest();
	    request.withMessageAttributes(messageAttributes);
	    request.setMessageBody("1");
	    this.amazonBobQueue.sendMessage(request);
	}
	
	// changed this to update it for the byte[]
	private void sendMessageToAlice(String transactionId, byte[] sigBob) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
    	
    	// changed this to update it for the byte[]
    	messageAttributes.put("sig-bob", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(sigBob)));
    	
    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringListValues(MessageStatus.TTP_to_Alice.getValue().toString()));
    	
    	SendMessageRequest request = new SendMessageRequest();
	    request.withMessageAttributes(messageAttributes);
	    request.setMessageBody("1");
	    this.amazonAliceQueue.sendMessage(request);
	}
	
	public void start() {
		if (thread == null) {
			thread = new Thread (this, "process messages");
			thread.start();
	    }
	}

	public void run() {
		while (true) {
			List<Message> messages = this.amazonTTPQueue.receiveMessages();
			for (Message message : messages) {
				String strMessageStatus = message.getAttributes().get("message-status").toString();
				Integer messageStatus = Integer.parseInt(strMessageStatus);
				
				if (messageStatus == MessageStatus.Alice_to_TTP.getValue()) {
					String transactionId = message.getAttributes().get("transaction-id").toString();
					
					// changed this to update it for the byte[]
					byte[] sigAlice = message.getAttributes().get("sig-alice").getBytes();
					String docKey = message.getAttributes().get("doc-key").toString();
					
					this.database.insertDataToTransactions(transactionId, sigAlice, docKey);
					this.sendMessageToBob(transactionId, sigAlice);
				}
				else if (messageStatus == MessageStatus.Bob_to_TTP.getValue()) {
					String transactionId = message.getAttributes().get("transaction-id").toString();
					
					// changed this to update it for the byte[]
					byte[] sigBob = message.getAttributes().get("sig-bob").getBytes();
					
					// WE NEED TO KNOW IF SIGBOB SENT BY BOB IS CORRECT
					/* to do this we need Bob's PublicKey.. could send it in the message or get Bob to transmit it to TTP another way? */
					
					String docKey = this.database.getDocKeyByTransactionId(transactionId);
					this.sendDocumentKeyToBob(transactionId, docKey);
					this.sendMessageToAlice(transactionId, sigBob);
					this.database.deleteFromTransactionsById(transactionId);
				}
			}
			
			this.amazonTTPQueue.deleteMessages();
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
