package org.ncl.cloudcomputing.common;

import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class AmazonQueue {

	private AmazonSQS sqs;
	
	private AWSCredentials credentials;
	
	private String queueName;
	
	private String queueUrl;
	
	public AmazonQueue(String queueName) {
		this.queueName = queueName;
		BasicAWSCredentials creds = new BasicAWSCredentials("AKIAJ4AF33GQN36VZPGA", "6BSToEQwvCMdfiBaSbG1kYpDL/lVPj1nMSQmGY1r"); 
		sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.EU_WEST_1).withCredentials(new AWSStaticCredentialsProvider(creds)).build();
		this.queueUrl = null;
		this.createQueue();
	}
	
	public boolean sendMessage(SendMessageRequest request) {
		try {
			request.withQueueUrl(this.queueUrl);
			this.sqs.sendMessage(request);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public List<Message> receiveMessages() {
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(this.queueUrl);
		receiveMessageRequest.withMessageAttributeNames("All");
		return this.sqs.receiveMessage(receiveMessageRequest).getMessages();
	}
	
	public void deleteMessages() {
		PurgeQueueRequest request = new PurgeQueueRequest(this.queueUrl);
		this.sqs.purgeQueue(request);
	}
	
	private void createQueue() {
		try {
			GetQueueUrlRequest request = new GetQueueUrlRequest().withQueueName(this.queueName);
			GetQueueUrlResult response = this.sqs.getQueueUrl(request);
			
			this.queueUrl = response.getQueueUrl();
			
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}

		CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(queueName);
		this.queueUrl = this.sqs.createQueue(createQueueRequest).getQueueUrl();
	}
}
