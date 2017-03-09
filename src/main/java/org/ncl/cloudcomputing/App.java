package org.ncl.cloudcomputing;

import java.util.List;
import java.util.UUID;

import org.ncl.cloudcomputing.common.AWSAuthentication;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class App 
{
	private static final String s3BucketName = UUID.randomUUID().toString();
	
    public static void main( String[] args )
    {
    	AWSCredentials credentials = AWSAuthentication.getAWSCredentials();
    	Bucket bucket = null;
    	
    	AmazonS3 s3 = new AmazonS3Client(credentials);
    	Region s3Region = Region.getRegion(Regions.EU_WEST_1);
        s3.setRegion(s3Region);
        
        BucketLifecycleConfiguration.Rule expirationRule = new BucketLifecycleConfiguration.Rule();
        expirationRule.withExpirationInDays(1).withStatus("Enabled");
        BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration().withRules(expirationRule);
        
        if (!s3.doesBucketExist(s3BucketName)) {
        	s3.createBucket(s3BucketName);
        	s3.setBucketLifecycleConfiguration(s3BucketName, lifecycleConfig);
        }
        else {
        	
        }
        
        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
        	      .withLargePayloadSupportEnabled(s3, s3BucketName);
        
        AmazonSQS sqsExtended = new AmazonSQSExtendedClient(new AmazonSQSClient(credentials), extendedClientConfig);
        Region sqsRegion = Region.getRegion(Regions.EU_WEST_1);
        sqsExtended.setRegion(sqsRegion);
        
        // get base64 format here
        byte[] b = new byte[1024];
        String str = new String(b);
        
        // Create a message queue for this example.
        String QueueName = "QueueName" + UUID.randomUUID().toString();
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(QueueName);
        String myQueueUrl = sqsExtended.createQueue(createQueueRequest).getQueueUrl();
        System.out.println("Queue created.");
        
        SendMessageRequest myMessageRequest = new SendMessageRequest(myQueueUrl, str);
        sqsExtended.sendMessage(myMessageRequest);
        System.out.println("Sent the message.");
        
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
        List<Message> messages = sqsExtended.receiveMessage(receiveMessageRequest).getMessages();
        
        // Delete the message, the queue, and the bucket.
        String messageReceiptHandle = messages.get(0).getReceiptHandle();
        sqsExtended.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));
        System.out.println("Deleted the message.");
        
        sqsExtended.deleteQueue(new DeleteQueueRequest(myQueueUrl));
        System.out.println("Deleted the queue.");
    }
}
