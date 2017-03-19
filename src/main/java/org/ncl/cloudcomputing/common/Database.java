package org.ncl.cloudcomputing.common;

import java.util.ArrayList;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

public class Database {

	private DynamoDB dynamoDB;
	
	private Table transactionTable;
	
	private static final String transactionTableName = "transactions";
	private static final String transactionTableIdColumn = "transaction_id";
	private static final String transactionTableSigColumn = "signature_alice";
	private static final String transactionTableDocKeyColumn = "document_key";
	
	
	public Database() {
		BasicAWSCredentials creds = new BasicAWSCredentials("AKIAJ4AF33GQN36VZPGA", "6BSToEQwvCMdfiBaSbG1kYpDL/lVPj1nMSQmGY1r"); 
		AmazonDynamoDBClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).build();
		this.transactionTable = this.createTransactionTable();
	}
	
	private Table createTransactionTable() {
		Table table = this.dynamoDB.getTable(transactionTableName);
		
		if (table == null) {
			ArrayList<AttributeDefinition> attributeDefinitions= new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition().withAttributeName(transactionTableIdColumn).withAttributeType("S"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName(transactionTableSigColumn).withAttributeType("B"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName(transactionTableDocKeyColumn).withAttributeType("S"));

			ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement().withAttributeName(transactionTableIdColumn).withKeyType(KeyType.HASH));
			        
			CreateTableRequest request = new CreateTableRequest()
			        .withTableName(transactionTableName)
			        .withKeySchema(keySchema)
			        .withAttributeDefinitions(attributeDefinitions)
			        .withProvisionedThroughput(new ProvisionedThroughput()
			            .withReadCapacityUnits(5L)
			            .withWriteCapacityUnits(6L));

			table = dynamoDB.createTable(request);

			try {
				table.waitForActive();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return table;
	}
	
	public void insertDataToTransactions(String transactionId, byte[] sigAlice, String docKey) {
		Item item = new Item()
				.withPrimaryKey(transactionTableIdColumn, transactionId)
				.withBinary(transactionTableSigColumn, sigAlice)
				.withString(transactionTableDocKeyColumn, docKey);
		
		this.transactionTable.putItem(item);
	}
	
	public String getDocKeyByTransactionId(String transactionId) {
		Item item = this.transactionTable.getItem(transactionTableIdColumn, transactionId);
		return item.get(transactionTableDocKeyColumn).toString();
	}
	
	public void deleteFromTransactionsById(String transactionId) {
		this.transactionTable.deleteItem(transactionTableIdColumn, transactionId);
	}
}
