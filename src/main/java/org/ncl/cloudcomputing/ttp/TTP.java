package org.ncl.cloudcomputing.ttp;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ncl.cloudcomputing.common.AWSBase;
import org.ncl.cloudcomputing.common.Logger;
import org.ncl.cloudcomputing.common.MessageStatus;
import org.ncl.cloudcomputing.common.TransactionItem;
import org.ncl.cloudcomputing.common.TransactionRepository;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class TTP extends AWSBase implements Runnable {
	
	private Thread thread;
	
	private TransactionRepository transactionRepo;
	
	public TTP() {
		this.transactionRepo = new TransactionRepository();
	}
	
	private void sendMessageToBob(String transactionId, byte[] sigAlice) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
		
    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(sigAlice)));
    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
    	
    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.TTP_to_Bob.getValue().toString()));
    	
    	SendMessageRequest request = new SendMessageRequest();
	    request.withMessageAttributes(messageAttributes);
	    request.setMessageBody("1");
	    this.amazonBobQueue.sendMessage(request, MessageStatus.TTP_to_Bob);
	}
	
	private void sendDocumentKeyToBob(String transactionId, String docKey, String filename) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
    	messageAttributes.put("file-name", new MessageAttributeValue().withDataType("String").withStringValue(filename));
    	
    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.TTP_to_Bob_doc.getValue().toString()));
    	
    	SendMessageRequest request = new SendMessageRequest();
	    request.withMessageAttributes(messageAttributes);
	    request.setMessageBody("1");
	    this.amazonBobQueue.sendMessage(request, MessageStatus.TTP_to_Bob_doc);
	}
	
	private void sendMessageToAlice(String transactionId, byte[] sigBob) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
    	messageAttributes.put("sig-bob", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(sigBob)));
    	
    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.TTP_to_Alice.getValue().toString()));
    	
    	SendMessageRequest request = new SendMessageRequest();
	    request.withMessageAttributes(messageAttributes);
	    request.setMessageBody("1");
	    this.amazonAliceQueue.sendMessage(request, MessageStatus.TTP_to_Alice);
	}
	
	public void start() {
		if (thread == null) {
			thread = new Thread (this, "process messages");
			thread.start();
	    }
	}

	public void run() {
		while (true) {
			System.out.println("Receiving messages...");
			
			List<Message> messages = this.amazonTTPQueue.receiveMessages();
			
			System.out.println(messages.size() + " messages received.");
			
			for (Message message : messages) {
				String strMessageStatus = message.getMessageAttributes().get("message-status").getStringValue();
				Integer messageStatus = Integer.parseInt(strMessageStatus);
				
				Logger.logReceiveMessageOnSucceed(messageStatus);
				
				if (messageStatus == MessageStatus.Alice_to_TTP.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					byte[] sigAlice = message.getMessageAttributes().get("sig-alice").getBinaryValue().array();
					String docKey = message.getMessageAttributes().get("doc-key").getStringValue();
					String filename = message.getMessageAttributes().get("file-name").getStringValue();
					
					TransactionItem item = new TransactionItem(transactionId, sigAlice, docKey, filename);
					TransactionItem result = this.transactionRepo.insert(item);
					
					if (result != null)
						this.sendMessageToBob(transactionId, sigAlice);
				}
				else if (messageStatus == MessageStatus.Bob_to_TTP.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					byte[] sigBob = message.getMessageAttributes().get("sig-bob").getBinaryValue().array();
					
					// WE NEED TO KNOW IF SIGBOB SENT BY BOB IS CORRECT
					/* to do this we need Bob's PublicKey.. could send it in the message or get Bob to transmit it to TTP another way? */
					
					TransactionItem transaction = this.transactionRepo.getItemById(transactionId);
					
					if (transaction != null) {
						this.sendDocumentKeyToBob(transactionId, transaction.getDocumentKey(), transaction.getFilename());
						this.sendMessageToAlice(transactionId, sigBob);
						this.transactionRepo.delete(transactionId);
					}
				}
			}
			
			//if (messages.size() != 0)
				//this.amazonTTPQueue.deleteMessages();
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
