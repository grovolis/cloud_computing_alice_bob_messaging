package org.ncl.cloudcomputing.common;

public abstract class AWSBase {

	private final static String ttpQueueName = "uk-ac-ncl-csc8109-team2-ttp-queue";
	private final static String aliceQueueName = "uk-ac-ncl-csc8109-team2-alice-queue";
	private final static String bobQueueName = "uk-ac-ncl-csc8109-team2-bob-queue";
	
	protected AmazonBucket amazonBucket;
	protected AmazonQueue amazonTTPQueue;
	protected AmazonQueue amazonAliceQueue;
	protected AmazonQueue amazonBobQueue; 
	
	public AWSBase(String profileName) {
		this.amazonBucket = new AmazonBucket(profileName);
		this.amazonTTPQueue = new AmazonQueue(ttpQueueName, profileName);
		this.amazonAliceQueue = new AmazonQueue(aliceQueueName, profileName);
		this.amazonBobQueue = new AmazonQueue(bobQueueName, profileName);
	}
}
