package org.ncl.cloudcomputing.common;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

public class TransactionRepository extends RepositoryBase<TransactionItem> {

	public TransactionRepository() {
		//this.createTableIfNotExists();
	}
	
	private void createTableIfNotExists() {
		DynamoDB dynamoDB = new DynamoDB(this.client);
		
		Table table = dynamoDB.getTable("Transactions");
		//System.out.println(table.getTableName());
		//TableCollection<ListTablesResult> tables = dynamoDB.listTables();
		
		if (table == null) {
			ArrayList<AttributeDefinition> attributeDefinitions= new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("Id").withAttributeType("S"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("Signature").withAttributeType("B"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("Document_Key").withAttributeType("S"));

			ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement().withAttributeName("Id").withKeyType(KeyType.HASH));
			        
			CreateTableRequest request = new CreateTableRequest()
			        .withTableName("Transactions")
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
	}
	
	@Override
	public TransactionItem getItemById(Object id) {
		if (!(id instanceof String)) {
			throw new IllegalArgumentException("Id must be String type!!");
		}
		
		String t_id = (String)id;
		
		System.out.println("It is fetching item by id...");
		System.out.println("Id: " + t_id);
		
		TransactionItem partitionKey = new TransactionItem(t_id);
		DynamoDBQueryExpression<TransactionItem> queryExpression = new DynamoDBQueryExpression<TransactionItem>().withHashKeyValues(partitionKey);
		List<TransactionItem> itemList = this.mapper.query(TransactionItem.class, queryExpression);
		
		System.out.println("Item fetched");
		
		return (itemList.size() == 0) ? null : itemList.get(0);
	}

	@Override
	public TransactionItem insert(TransactionItem item) {
		System.out.println("Data is being inserted to database...");
		System.out.println("Id: " + item.getId());
		
		try {
			this.mapper.save(item);
		} catch (Exception e) {
			System.out.println("Data could not be inserted");
			e.printStackTrace();
			return null;
		}
		
		System.out.println("Data was inserted");
		
		return item;
	}

	@Override
	public TransactionItem delete(Object id) {
		if (!(id instanceof String)) {
			throw new IllegalArgumentException("Id must be String type!!");
		}
		
		String t_id = (String)id;
		
		System.out.println("Data is being deleted from database...");
		System.out.println("Id: " + t_id);
		
		TransactionItem partitionKey = new TransactionItem(t_id);
		
		try {
			this.mapper.delete(partitionKey);
		} catch (Exception e) {
			System.out.println("Data could not be deleted");
			e.printStackTrace();
			return null;
		}
		
		System.out.println("Data was deleted");
		
		return partitionKey;
	}

}
