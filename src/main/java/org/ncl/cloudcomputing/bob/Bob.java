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
	private byte[] signature;
	private ArrayList<String> transactions;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	
	public Bob() {
		this.transactions = new ArrayList<String>(); 
		try {
			makeRSAKeyPair(2048);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Allows a third party to use the clients PublicKey
	 * @return PublicKey
	 */
	public PublicKey getPublicKey() {
		return publicKey;
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
			rsa = Signature.getInstance("RSA");
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
	 
	
	private boolean sendMessageToTTP(String transactionId) {
		try {
			if (this.signature == null) return false;
			
			Map<String, MessageAttributeValue> messageAttributes = new HashMap<String, MessageAttributeValue>();

			messageAttributes.put("transaction-id", new MessageAttributeValue().withDataType("String").withStringValue(transactionId));
	    	messageAttributes.put("sig-bob", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(signature)));
	    	messageAttributes.put("message-status", new MessageAttributeValue().withDataType("String").withStringListValues(MessageStatus.Bob_to_TTP.getValue().toString()));
	    	messageAttributes.put("public-key", new MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(this.publicKey.getEncoded())));
	    	
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
					
					// changed this to update it for the byte[]
					byte[] sigAlice = message.getAttributes().get("sig-alice").getBytes();
					
					/* this sig will be { sigB(sigA(H(doc))) } e.g. Bob signature over alice's signature of the hashed document*/
					this.signature = generateRSASignature(sigAlice);
					
					
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
