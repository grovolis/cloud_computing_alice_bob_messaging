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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ncl.cloudcomputing.common.AWSBase;
import org.ncl.cloudcomputing.common.Logger;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Alice extends AWSBase implements Runnable {
	
	private ArrayList<String> transactions;
	private Thread thread;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	private byte[] signature;
	
	public Alice() {
		this.transactions = new ArrayList<String>(); 
		try {
			makeRSAKeyPair(2048);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	/**
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
	 * Hashes a file
	 * @param file to hash
	 * @return byte[]
	 */
	private byte[] hashFile(File file) {
		MessageDigest md;
		byte[] data;
		byte[] hash = null;
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
	
	public String putObjectToBucket(String filename) {
		
		File file = new File(filename);
		
		if(!file.isFile()) { 
		    throw new IllegalArgumentException("the file does not exist");
		}
		byte[] hash = hashFile(file);
		signature = generateRSASignature(hash);
		
		return amazonBucket.storeObject(file);
	}
	
	public boolean sendMessageToTTP(String docKey, String filename) {
		try {
			if (this.signature == null) return false;
			
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();
			
			String transactionId = UUID.randomUUID().toString();

	    	messageAttributes.put("doc-key", new MessageAttributeValue().withDataType("String").withStringValue(docKey));
	    	messageAttributes.put("sig-alice", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(signature)));
	    	messageAttributes.put("public-key", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(this.publicKey.getEncoded())));
	    	messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("file-name", new MessageAttributeValue().withDataType("String").withStringValue(filename));
	    	
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("Number").withStringValue(MessageStatus.Alice_to_TTP.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("Alice To TTP");
		    this.amazonTTPQueue.sendMessage(request, MessageStatus.Alice_to_TTP);
		    
		    transactions.add(transactionId);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void run() {
		
		while (true) {
			Logger.log("Receiving messages...");
			
			List<Message> messages = this.amazonAliceQueue.receiveMessages();
			
			if (messages.size() > 0)
				Logger.log(messages.size() + " messages received.");
			else 
				Logger.log("No message to process.");
			
			for (Message message : messages) {
				String strMessageStatus = message.getAttributes().get("message-status").toString();
				Integer messageStatus = Integer.parseInt(strMessageStatus);
				
				Logger.logReceiveMessageOnSucceed(messageStatus);
				
				if (messageStatus == MessageStatus.TTP_to_Alice.getValue()) {
					String transactionId = message.getAttributes().get("transaction-id").toString();
					byte[] sigBob = message.getAttributes().get("sig-bob").getBytes();
					transactions.remove(transactionId);
				}
				
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
	
	public void start() {
		
		Logger.log("Alice is getting ready to process messages...");
		
		if (thread == null) {
			thread = new Thread (this, "process messages");
			thread.start();
	    }
		
		Logger.log("Alice is ready to process messages...");
	}
}
