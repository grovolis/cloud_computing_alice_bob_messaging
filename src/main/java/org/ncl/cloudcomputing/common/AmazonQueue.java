package org.ncl.cloudcomputing.common;

import java.util.List;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * @author alper
 * The class implements operations for the queues in the cloud.
 */
public class AmazonQueue {

	private AmazonSQS sqs;
	private String queueName;
	private String queueUrl;
	
	/**
	 * @param queueName: name of the queue, there are three queues. 
	 * @param profileName: each entity (Alice, Bob, & TTP) uses different profile names because of the restrictions
	 * e.g. Alice does not have permission for read, delete, & write on Bob's queue.
	 */
	public AmazonQueue(String queueName, String profileName) {
		this.queueName = queueName;
		
		// Connect to Amazon SQS components with the access key.
		// Eclipse provides storage for access keys marked with a profile name .
		this.sqs = new AmazonSQSClient(new ProfileCredentialsProvider(profileName));
		this.sqs.setRegion(Region.getRegion(Regions.EU_WEST_1));
		
		this.queueUrl = null;
		this.createQueue();
	}
	
	public boolean sendMessage(SendMessageRequest request, MessageStatus status) {
		try {
			Logger.logSendMessageOnProcess(status);
			
			request.withQueueUrl(this.queueUrl);
			this.sqs.sendMessage(request);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.logSendMessageOnFail(status);
			return false;
		}
		
		Logger.logSendMessageOnSucceed(status);
		return true;
	}
	
	public List<Message> receiveMessages() {
		try {
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(this.queueUrl);
			receiveMessageRequest.withMessageAttributeNames("All");
			return this.sqs.receiveMessage(receiveMessageRequest).getMessages();		
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void deleteMessage(String messageReceiptHandle) {
		try {
			Logger.log("The message was processed. It is being deleted...");
			DeleteMessageRequest request = new DeleteMessageRequest(this.queueUrl, messageReceiptHandle);
			this.sqs.deleteMessage(request);
		} catch (Exception e) {
			Logger.log("The message could not be deleted!!!");
			e.printStackTrace();
		}
		
		Logger.log("The message was deleted.");
	}
	
	/**
	 * Create a queue with a queue name, if it does not exist.
	 * It gives a queue url.
	 */
	private void createQueue() {
		Logger.log("Checking the queue - " + this.queueName);
		
		try {
			GetQueueUrlRequest request = new GetQueueUrlRequest().withQueueName(this.queueName);
			GetQueueUrlResult response = this.sqs.getQueueUrl(request);
			
			this.queueUrl = response.getQueueUrl();
			
			Logger.log("The queue was found");
			Logger.log("The queue url: " + this.queueUrl);
			
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Logger.log("The queue could not be found!!!");
		Logger.log("New queue is being created...");

		try {
			CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(this.queueName);
			this.queueUrl = this.sqs.createQueue(createQueueRequest).getQueueUrl();
		}
		catch (Exception e) {
			Logger.log("The queue could not be created!!!");
			e.printStackTrace();
		}
		
		Logger.log("The queue was created.");
		Logger.log("The queue url: " + this.queueUrl);
	}
}
