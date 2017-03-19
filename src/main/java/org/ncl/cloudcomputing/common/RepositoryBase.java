package org.ncl.cloudcomputing.common;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public abstract class RepositoryBase<T> {
	
	protected DynamoDBMapper mapper;
	
	public RepositoryBase() {
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(new ProfileCredentialsProvider());
		DynamoDBMapper mapper = new DynamoDBMapper(client);
	}
	
	public abstract T getItemById(Object id);
	
	public abstract T insert(T item);
	
	public abstract T delete(Object id);
}
