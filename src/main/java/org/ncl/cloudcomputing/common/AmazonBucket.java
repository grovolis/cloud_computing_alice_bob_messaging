package org.ncl.cloudcomputing.common;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class AmazonBucket {
	
	private static final String bucketName = "uk.ac.ncl.csc8109.team2.bucket";
	private HashMap<String, String> storedFiles;
	
	private AmazonS3 s3client;
	
	public AmazonBucket() {
		this.storedFiles = new HashMap<String, String>();
		
		/* deprecated? */
		this.s3client = new AmazonS3Client(new ProfileCredentialsProvider());
		this.s3client.setRegion(Region.getRegion(Regions.EU_WEST_1));
		
		/* changed this to use non deprecated client builder */
		//BasicAWSCredentials creds = new BasicAWSCredentials("AKIAJ4AF33GQN36VZPGA", "6BSToEQwvCMdfiBaSbG1kYpDL/lVPj1nMSQmGY1r"); 
		//s3client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).withCredentials(new AWSStaticCredentialsProvider(creds)).build();
		this.createBucket();
	}
	
	private Bucket createBucket() {
		Bucket b = null;
		
		Logger.log("Checking the bucket - " + bucketName);
		
		List<Bucket> buckets = s3client.listBuckets();
		
		if (buckets.size() == 0) {
			
			Logger.log("The queue could not find.");
			Logger.log("New queue is being created...");
			
			try {
				b = s3client.createBucket(new CreateBucketRequest(bucketName));
				
				BucketLifecycleConfiguration.Rule expirationRule = new BucketLifecycleConfiguration.Rule();
		        expirationRule.withExpirationInDays(1).withStatus("Enabled");
		        BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration().withRules(expirationRule);
		        
				s3client.setBucketLifecycleConfiguration(bucketName, lifecycleConfig);
			} catch (Exception e) {
				Logger.log("The bucket could not be created!!!");
				e.printStackTrace();
			}
			
			Logger.log("The bucket was created.");
		}
		else {
			Logger.log("The bucket was found");
		}
		
		
		return b;
	}
	
	public String storeObject(File file) {
		String key = this.generateKey();
		
		Logger.log("Alice is sending the file to bucket...");
		Logger.log("Filename: " + file.getName());
		
		try {
			s3client.putObject(new PutObjectRequest(bucketName, key, file));
	        storedFiles.put(key, file.getName());
		}
		catch (Exception e) {
			Logger.log("The file could not be sent!!!");
			e.printStackTrace();
		}
        
        Logger.log("The file was sent.");
		
        return key;
	}
	
	public void deleteObject(String docKey) {
		Logger.log("The file is being deleted...");
		Logger.log("The document key: " + docKey);
		
		try {
			s3client.deleteObject(new DeleteObjectRequest(bucketName, docKey));	
		}
		catch (Exception e) {
			Logger.log("The file could not be deleted!!!");
			e.printStackTrace();
		}
		
		Logger.log("The file was deleted.");
	}
	
	public S3Object getObject(String docKey) {
		
		Logger.log("The file info is being retrieved from the bucket...");
		Logger.log("The document key: " + docKey);
		
		try {
			S3Object s3Object = s3client.getObject(new GetObjectRequest(bucketName, docKey));
			Logger.log("The file info was retrieved from the bucket!!!");
			return s3Object;
		}
		catch (Exception e) {
			Logger.log("The file info could not be retrieved from the bucket!!!");
			e.printStackTrace();
			return null;
		}
	}
	
	private String generateKey() {
		return UUID.randomUUID().toString();
	}
}
