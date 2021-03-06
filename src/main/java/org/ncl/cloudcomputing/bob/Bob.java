package org.ncl.cloudcomputing.bob;

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

import org.ncl.cloudcomputing.common.AWSBase;
import org.ncl.cloudcomputing.common.Logger;
import org.ncl.cloudcomputing.common.MessageStatus;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * @author alper
 * The class represents Bob.
 * Bob receives a document from Alice via TTP running in the cloud and sends its signature.
 * Then, he listens her queue in the cloud to see messages.
 */
public class Bob extends AWSBase implements Runnable {

	// see alice
	private Thread thread;
	private byte[] signature;
	private ArrayList<String> transactions;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	
	public Bob() {
		super("bob");
		this.transactions = new ArrayList<String>(); 
		try {
			makeRSAKeyPair(2048);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @author ryan
	 * Allows a third party to use the clients PublicKey
	 * @return PublicKey
	 */
	public PublicKey getPublicKey() {
		return publicKey;
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
	
	public boolean registerPublicKey() {
		try {
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

			messageAttributes.put("client-name", new MessageAttributeValue().withDataType("String").withStringValue("Bob"));
			messageAttributes.put("public-key", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(this.publicKey.getEncoded())));
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringValue(MessageStatus.Register.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("Bob to TTP and Alice (Register)");
		    this.amazonTTPQueue.sendMessage(request, MessageStatus.Register);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	/**
	 * This methods sends SigB(SigA(h(doc))) to TTP
	 * @param transactionId: id of the transaction 
	 * @return true if successful
	 */
	private boolean sendMessageToTTP(String transactionId) {
		try {
			if (this.signature == null) return false;
			
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

			messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("sig-bob", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(this.signature)));
	    	
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringValue(MessageStatus.Bob_to_TTP.getValue().toString()));
	    	
	    	SendMessageRequest request = new SendMessageRequest();
		    request.withMessageAttributes(messageAttributes);
		    request.setMessageBody("Bob to TTP");
		    this.amazonTTPQueue.sendMessage(request, MessageStatus.Bob_to_TTP);
		    
		    transactions.add(transactionId);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	/**
	 * This method saves the document from the bucket to Bob's local
	 * 
	 * @param filename: name of the document to be saved
	 * @param docKey: the key to get the document from the bucket 
	 * @return true if successful
	 */
	private boolean copyDocumentToLocal(String filename, String docKey) {
		Logger.log("Bob is copying the file to local");
		Logger.log("Filename: " + filename);
		
		S3Object object = this.amazonBucket.getObject(docKey);
		
		String path = System.getProperty("user.dir") + "\\bob-files\\" + filename;
		
		try {
			Files.copy(object.getObjectContent(), new File(path).toPath());
			object.close();
			Logger.log("Bob copied the file to local");
			
			this.amazonBucket.deleteObject(docKey);
		} catch (IOException e) {
			Logger.log("Bob could not copy the file to local!!!");
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
			
			List<Message> messages = this.amazonBobQueue.receiveMessages();
			
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
				
				// If TTP sends SigA(h(doc))
				if (messageStatus == MessageStatus.TTP_to_Bob.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					byte[] sigAlice = message.getMessageAttributes().get("sig-alice").getBinaryValue().array();
					
					Logger.log("Transaction id: " + transactionId);
					
					/* this sig will be { sigB(sigA(H(doc))) } e.g. Bob signature over alice's signature of the hashed document*/
					this.signature = generateRSASignature(sigAlice);
					
					this.sendMessageToTTP(transactionId);
					transactions.add(transactionId);
				}
				// If TTP sends document info
				else if (messageStatus == MessageStatus.TTP_to_Bob_doc.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					String docKey = message.getMessageAttributes().get("doc-key").getStringValue();
					String filename = message.getMessageAttributes().get("file-name").getStringValue();
					
					if (this.copyDocumentToLocal(filename, docKey)) {
						Logger.log("The transaction has been completed successfully");
						Logger.log("The transaction id: " + transactionId);
					}
				}
				// If TTP sends a termination message
				else if (messageStatus == MessageStatus.Transaction_Terminate.getValue()) {
					String transactionId = message.getMessageAttributes().get("transaction-id").getStringValue();
					transactions.remove(transactionId);
					
					Logger.log("The transaction was terminated by TTP because of security violation.");
					Logger.log("The transaction id: " + transactionId);
				}
				
				// clean processed messages in the queue
				this.amazonBobQueue.deleteMessage(message.getReceiptHandle());
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
		
		Logger.log("Bob is getting ready to process messages...");
		
		if (thread == null) {
			thread = new Thread (this, "process messages");
			thread.start();
	    }
		
		Logger.log("Bob is ready to process messages...");
	}
}
