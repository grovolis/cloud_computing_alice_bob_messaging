package org.ncl.cloudcomputing.common;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AmazonBucket {
	
	private static final String bucketName = "simple_bucket";
	private HashMap<String, String> storedFiles;
	
	private AmazonS3 s3client;
	
	public AmazonBucket() {
		this.storedFiles = new HashMap<String, String>();
		this.s3client = new AmazonS3Client(new ProfileCredentialsProvider());
		this.s3client.setRegion(Region.getRegion(Regions.EU_WEST_1));
		
		this.createBucket();
	}
	
	private Bucket createBucket() {
		Bucket b = null;
		if (!s3client.doesBucketExist(bucketName)) {
			b = s3client.createBucket(new CreateBucketRequest(bucketName));
			
			BucketLifecycleConfiguration.Rule expirationRule = new BucketLifecycleConfiguration.Rule();
	        expirationRule.withExpirationInDays(1).withStatus("Enabled");
	        BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration().withRules(expirationRule);
	        
			s3client.setBucketLifecycleConfiguration(bucketName, lifecycleConfig);
		}
		
		return b;
	}
	
	public String storeObject(String fileName) {
		String key = this.generateKey();
		File file = new File(fileName);
		
		if(file.exists() && !file.isDirectory()) { 
		    throw new IllegalArgumentException("the file does not exist");
		}
		
        s3client.putObject(new PutObjectRequest(bucketName, key, file));
        storedFiles.put(key, fileName);
        
        return key;
	}
	
	private String generateKey() {
		return UUID.randomUUID().toString();
	}
}
