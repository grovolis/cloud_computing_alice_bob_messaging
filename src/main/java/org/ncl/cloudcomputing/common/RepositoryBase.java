package org.ncl.cloudcomputing.common;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

public abstract class RepositoryBase<T> {
	
	protected DynamoDBMapper mapper;
	
	protected AmazonDynamoDB client;
	
	public RepositoryBase() {
		
//		this.client = new AmazonDynamoDBClient(new ProfileCredentialsProvider());
//		this.client.setRegion(Region.getRegion(Regions.EU_WEST_1));
		
		this.client = AmazonDynamoDBClientBuilder.standard()
				.withRegion(Regions.EU_WEST_1)
				.withCredentials(new ProfileCredentialsProvider("default"))
				.build();
		
		this.mapper = new DynamoDBMapper(this.client);
	}
	
	public abstract T getItemById(Object id);
	
	public abstract T insert(T item);
	
	public abstract T delete(Object id);
}
