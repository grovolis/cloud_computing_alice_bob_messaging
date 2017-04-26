package org.ncl.cloudcomputing.common;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

/**
 * @author alper
 * Base class for all repository classes to implement DynamoDB operations.
 * @param <T>: Class definition of the item (table) in DynamoDB.
 */
public abstract class RepositoryBase<T> {
	
	protected DynamoDBMapper mapper;
	
	protected AmazonDynamoDB client;
	
	public RepositoryBase() {
		
		// Connect to DynamoDB components with the access key.
		// Eclipse provides storage for access keys marked with a profile name .
		this.client = AmazonDynamoDBClientBuilder.standard()
				.withRegion(Regions.EU_WEST_1)
				.withCredentials(new ProfileCredentialsProvider("ttp"))
				.build();
		
		this.mapper = new DynamoDBMapper(this.client);
	}
	
	public abstract T getItemById(Object id);
	
	public abstract T insert(T item);
	
	public abstract T delete(Object id);
}
