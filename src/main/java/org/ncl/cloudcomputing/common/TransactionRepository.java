package org.ncl.cloudcomputing.common;

import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;

public class TransactionRepository extends RepositoryBase<TransactionItem> {

	public TransactionRepository() {
		
	}
	
	@Override
	public TransactionItem getItemById(Object id) {
		if (!(id instanceof String)) {
			throw new IllegalArgumentException("Id must be String type!!");
		}
		
		String t_id = (String)id;
		
		Logger.log("It is fetching item by id...");
		Logger.log("Id: " + t_id);
		
		TransactionItem partitionKey = new TransactionItem(t_id);
		DynamoDBQueryExpression<TransactionItem> queryExpression = new DynamoDBQueryExpression<TransactionItem>().withHashKeyValues(partitionKey);
		List<TransactionItem> itemList = this.mapper.query(TransactionItem.class, queryExpression);
		
		Logger.log("Item fetched");
		
		return (itemList.size() == 0) ? null : itemList.get(0);
	}

	@Override
	public TransactionItem insert(TransactionItem item) {
		Logger.log("Data is being inserted to database...");
		Logger.log("Id: " + item.getId());
		
		try {
			this.mapper.save(item);
		} catch (Exception e) {
			Logger.log("Data could not be inserted");
			e.printStackTrace();
			return null;
		}
		
		Logger.log("Data was inserted");
		
		return item;
	}

	@Override
	public TransactionItem delete(Object id) {
		if (!(id instanceof String)) {
			throw new IllegalArgumentException("Id must be String type!!");
		}
		
		String t_id = (String)id;
		
		Logger.log("Data is being deleted from database...");
		Logger.log("Id: " + t_id);
		
		TransactionItem partitionKey = new TransactionItem(t_id);
		
		try {
			this.mapper.delete(partitionKey);
		} catch (Exception e) {
			Logger.log("Data could not be deleted");
			e.printStackTrace();
			return null;
		}
		
		Logger.log("Data was deleted");
		
		return partitionKey;
	}

}
