package org.ncl.cloudcomputing.ttp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
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
	private HashMap<String, byte[]> publicKeys;
	
	public TTP() {
		super("ttp");
		this.publicKeys = new HashMap<String, byte[]>();
		this.transactionRepo = new TransactionRepository();
	}
	
	/**
	 * sends sigA(h(doc)) to Bob.
	 * @param transactionId: id of the transaction
	 * @param sigAlice: sigA(h(doc))
	 * @return true if successful
	 */
	private boolean sendMessageToBob(String transactionId, byte[] sigAlice) {
		try {
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
			
			// sigA(h(doc))
	    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(sigAlice)));
	    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.TTP_to_Bob.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("TTP to Bob");
		    this.amazonBobQueue.sendMessage(request, MessageStatus.TTP_to_Bob);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * TTP sends the document key to Bob
	 * @param transactionId: id of the transaction
	 * @param docKey: the key in the bucket to access the document
	 * @param filename: name of the document
	 * @return true if successful
	 */
	private boolean sendDocumentKeyToBob(String transactionId, String docKey, String filename) {
		try {
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

	    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
	    	messageAttributes.put("file-name", new MessageAttributeValue().withDataType("String").withStringValue(filename));
	    	
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.TTP_to_Bob_doc.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("TTP to Bob - document");
		    this.amazonBobQueue.sendMessage(request, MessageStatus.TTP_to_Bob_doc);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * TTP sends the signature of Bob to Alice
	 * @param transactionId: id of the transaction
	 * @param sigBob: sigB(sigA(h(doc)))
	 * @return true if successful
	 */
	private boolean sendMessageToAlice(String transactionId, byte[] sigBob) {
		try {
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

	    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	// sigB(sigA(h(doc)))
	    	messageAttributes.put("sig-bob", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(sigBob)));
	    	
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.TTP_to_Alice.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("TTP to Alice");
		    this.amazonAliceQueue.sendMessage(request, MessageStatus.TTP_to_Alice);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * TTP sends termination messages to Alice and Bob
	 * @param transactionId: id of the transaction
	 * @return true if successful
	 */
	private boolean sendTerminateMessages(String transactionId) {
		try {
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

	    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.Transaction_Terminate.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("Termination message");
		    
		    this.amazonAliceQueue.sendMessage(request, MessageStatus.Transaction_Terminate);
		    this.amazonBobQueue.sendMessage(request, MessageStatus.Transaction_Terminate);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	public void start() {
		Logger.log("TTP is starting...");
		
		if (thread == null) {
			thread = new Thread (this, "process messages");
			thread.start();
	    }
		
		Logger.log("TTP started");
	}
	
	/**
	 * @param signature: signature to be verified
	 * @param docHash: h(doc)
	 * @param publicKeyBytes: public key of the owner of the signature
	 * @return
	 */
	private boolean verifySignature(byte[] signature, byte[] docHash, byte[] publicKeyBytes) {
		
		if (publicKeyBytes == null) return false;
		
		PublicKey publicKey;
		Signature sig;
		try {
			publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
			sig = Signature.getInstance("SHA256withRSA");
			sig.initVerify(publicKey);
			sig.update(docHash);
			return sig.verify(signature);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * Receive messages in a thread with two seconds interval.
	 */
	public void run() {
		while (true) {
			Logger.log("Receiving messages...");
			
			List<Message> messages = this.amazonTTPQueue.receiveMessages();
			
			if (messages.size() > 0)
				Logger.log(messages.size() + " messages received.");
			else 
				Logger.log("No message to process.");
			
			for (Message message : messages) {
				// read status of a message
				// react regarding the status 
				String strMessageStatus = message.getMessageAttributes().get("message-status").getStringValue();
				Integer messageStatus = Integer.parseInt(strMessageStatus);
				
				Logger.logReceiveMessageOnSucceed(messageStatus);
				
				// If Alice sends SigA(h(doc)) to TTP.
				if (messageStatus == MessageStatus.Alice_to_TTP.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					byte[] sigAlice = message.getMessageAttributes().get("sig-alice").getBinaryValue().array();
					byte[] docHash = message.getMessageAttributes().get("doc-hash").getBinaryValue().array();
					String docKey = message.getMessageAttributes().get("doc-key").getStringValue();
					String filename = message.getMessageAttributes().get("file-name").getStringValue();
					
					Logger.log("Transaction id: " + transactionId);
					
					byte[] publicKeyAlice = this.publicKeys.get("Alice");
					if (!this.publicKeys.containsKey("Alice")) {
						Logger.log("Alice has not registered the public key yet.");
						Logger.log("Therefore, message will be read again.");
						
						continue;
					}
					
					// Verifies sigA(h(doc))
					if(verifySignature(sigAlice, docHash, publicKeyAlice)) {
						// Save the transaction info to DB to retrieve in late stages.
						TransactionItem item = new TransactionItem(transactionId, sigAlice, docHash, docKey, filename);
						TransactionItem result = this.transactionRepo.insert(item);
						
						if (result != null)
							this.sendMessageToBob(transactionId, sigAlice);
					}
					// If sigA(h(doc)) is not verified, terminate the transaction
					else {
						Logger.log("Alice's signature could not be verified.");
						Logger.log("Transaction will be terminated");
						Logger.log("Transaction id: " + transactionId);
						
						this.sendTerminateMessages(transactionId);
						this.transactionRepo.delete(transactionId);
						this.amazonBucket.deleteObject(docKey);
					}
				}
				// If Bob sends sigB(sigA(h(doc))) to TTP
				else if (messageStatus == MessageStatus.Bob_to_TTP.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					byte[] sigBob = message.getMessageAttributes().get("sig-bob").getBinaryValue().array();
					
					Logger.log("Transaction id: " + transactionId);
					
					TransactionItem transaction = this.transactionRepo.getItemById(transactionId);
					
					byte[] publicKeyBob = this.publicKeys.get("Bob");
					if (!this.publicKeys.containsKey("Bob")) {
						Logger.log("Bob has not registered the public key yet.");
						Logger.log("Therefore, the message will be read again.");
						
						continue;
					}
					
					// Verifies sigB(sigA(h(doc)))
					if(verifySignature(sigBob, transaction.getSignature(), publicKeyBob)) {
						if (transaction != null) {
							// Send the document key to Bob and the signature of Bob to Alice, after then delete the record from database.
							if (this.sendDocumentKeyToBob(transactionId, transaction.getDocumentKey(), transaction.getFilename()))
								if (this.sendMessageToAlice(transactionId, sigBob))
									this.transactionRepo.delete(transactionId);
						}
					}
					// If sigB(sigA(h(doc))) is not verified, terminate the transaction
					else {
						Logger.log("Bob's signature could not be verified.");
						Logger.log("Transaction will be terminated");
						Logger.log("Transaction id: " + transactionId);
						
						this.sendTerminateMessages(transactionId);
						this.transactionRepo.delete(transactionId);
						this.amazonBucket.deleteObject(transaction.getDocumentKey());
					}
				}
				// If one of entities sends its public key.
				else if (messageStatus == MessageStatus.Register.getValue()) {
					String client = message.getMessageAttributes().get("client-name").getStringValue();
					byte[] publicKey = message.getMessageAttributes().get("public-key").getBinaryValue().array();
					
					Logger.log("Client name is " + client);
					
					if (this.publicKeys.containsKey(client)) {
						this.publicKeys.remove(client);
					}
					this.publicKeys.put(client, publicKey);
				}
				
				// clean processed messages in the queue
				this.amazonTTPQueue.deleteMessage(message.getReceiptHandle());
			}
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
