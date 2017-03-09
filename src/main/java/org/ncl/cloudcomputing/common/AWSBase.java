package org.ncl.cloudcomputing.common;

public abstract class AWSBase {

	private final static String ttpQueueName = "queue-ttp";
	private final static String aliceQueueName = "queue-alice";
	private final static String bobQueueName = "queue-bob";
	
	protected AmazonBucket amazonBucket;
	protected AmazonQueue amazonTTPQueue;
	protected AmazonQueue amazonAliceQueue;
	protected AmazonQueue amazonBobQueue; 
	
	public AWSBase() {
		this.amazonBucket = new AmazonBucket();
		this.amazonTTPQueue = new AmazonQueue(ttpQueueName);
		this.amazonAliceQueue = new AmazonQueue(aliceQueueName);
		this.amazonBobQueue = new AmazonQueue(bobQueueName);
	}
}
