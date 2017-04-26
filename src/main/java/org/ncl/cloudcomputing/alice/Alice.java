package org.ncl.cloudcomputing.alice;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.ncl.cloudcomputing.common.AWSBase;
import org.ncl.cloudcomputing.common.Logger;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * @author alper
 * The class represents Alice.
 * Alice sends a document to Bob via TTP running in the cloud and starts a transaction.
 * Then, she listens her queue in the cloud to see messages.
 * At the end of a transaction, Alice receives the signature of Bob via TTP.
 */
public class Alice extends AWSBase implements Runnable {
	
	// Store transactions
	private ArrayList<String> transactions;
	// The thread that alice receives and reacts messages.
	private Thread thread;
	// Alice produces a public key to verify its private key
	private PublicKey publicKey;
	// Alice produces a private key to have a unique signature
	private PrivateKey privateKey;
	// Unique signature of Alice
	private byte[] signature;
	// Hash of the document that will be sent.
	private byte[] hash;
	
	public Alice() {
		super("alice");
		this.transactions = new ArrayList<String>(); 
		try {
			makeRSAKeyPair(2048);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @author ryan
	 * Generates an RSA keypair of the specified length and assigns them as instance variables
	 * @param keyLength int
	 * @throws GeneralSecurityException
	 */
	private void makeRSAKeyPair(int keyLength) throws GeneralSecurityException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
	    keyPairGenerator.initialize(keyLength);
	    KeyPair keyPair = keyPairGenerator.generateKeyPair();
	    privateKey = keyPair.getPrivate();
	    publicKey = keyPair.getPublic();
	}
	
	/**
	 * @author ryan
	 * Generates a signature over some data
	 * @param data to be signed
	 * @return a byte[] the signature
	 */
	private byte[] generateRSASignature(byte[] data) {
		Signature rsa;
		byte[] sig = null;
		try {
			rsa = Signature.getInstance("SHA256withRSA");
			rsa.initSign(privateKey, new SecureRandom());
			rsa.update(data);
			sig = rsa.sign();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		return sig;
	}
	
	/**
	 * @author ryan
	 * Hashes a file
	 * @param file to hash
	 * @return byte[]
	 */
	private byte[] hashFile(File file) {
		MessageDigest md;
		byte[] data;
		try {
			md = MessageDigest.getInstance("SHA-256");
			Path path = file.toPath();
			data = Files.readAllBytes(path);
			md.update(data);
			hash = md.digest();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return hash;
	}
	
	/**
	 * this method stores a file from local memory to the bucket in the cloud. 
	 * 
	 * @param filename: name of the file in local
	 * @return key of the file stored in the bucket
	 */
	public String putObjectToBucket(String filename) {
		
		File file = new File(filename);
		
		if(!file.isFile()) { 
		    throw new IllegalArgumentException("the file does not exist");
		}
		this.hash = hashFile(file);
		signature = generateRSASignature(this.hash);
		
		return amazonBucket.storeObject(file);
	}
	
	/**
	 * This method sends public key of Alice to TTP.
	 * @return true if successful.
	 */
	public boolean registerPublicKey() {
		try {
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

			messageAttributes.put("client-name", new MessageAttributeValue().withDataType("String").withStringValue("Alice"));
	    	messageAttributes.put("public-key", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(this.publicKey.getEncoded())));
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringValue(MessageStatus.Register.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("Bob to TTP (Register)");
		    this.amazonTTPQueue.sendMessage(request, MessageStatus.Register);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	/**
	 * This method sends the document key and file name to TTP.
	 * 
	 * @param docKey: the key of the document stored in the bucket.
	 * @param filename: name of the document  
	 * @return true if successful
	 */
	public boolean sendMessageToTTP(String docKey, String filename) {
		try {
			if (this.signature == null) return false;
			
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
			
			// a transaction id is produced for each transaction to distinguish it.
			String transactionId = UUID.randomUUID().toString();
			
			// the key to get the document from the bucket
	    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
	    	// H(doc)
			messageAttributes.put("doc-hash", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(this.hash)));
			// sigA(H(doc))
	    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(signature)));
	    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("file-name", new MessageAttributeValue().withDataType("String").withStringValue(filename));
	    	
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.Alice_to_TTP.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("Alice To TTP");
		    this.amazonTTPQueue.sendMessage(request, MessageStatus.Alice_to_TTP);
		    
		    Logger.log("A transaction was started");
		    Logger.log("Transaction id: " + transactionId);
		    
		    transactions.add(transactionId);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * 
	 * Receive messages in a thread with two seconds interval.
	 */
	public void run() {
		
		while (true) {
			Logger.log("Receiving messages...");
			
			List<Message> messages = this.amazonAliceQueue.receiveMessages();
			
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
				
				// If TTP sends SigB(SigA(h(doc)))
				if (messageStatus == MessageStatus.TTP_to_Alice.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					byte[] sigBob = message.getMessageAttributes().get("sig-bob").getBinaryValue().array();
					
					Logger.log("Signature Bob has been received!!!");
					
					Logger.log("The transaction has been completed successfully");
					Logger.log("The transaction id: " + transactionId);
					
					transactions.remove(transactionId);
				}
				// If TTP sends a termination message
				else if (messageStatus == MessageStatus.Transaction_Terminate.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					transactions.remove(transactionId);
					
					Logger.log("The transaction was terminated by TTP because of security violation.");
					Logger.log("The transaction id: " + transactionId);
				}
				
				// clean processed messages in the queue
				this.amazonAliceQueue.deleteMessage(message.getReceiptHandle());
			}
			
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Starts the thread.
	 */
	public void start() {
		
		Logger.log("Alice is getting ready to process messages...");
		
		if (thread == null) {
			thread = new Thread (this, "process messages");
			thread.start();
	    }
		
		Logger.log("Alice is ready to process messages...");
	}
}
