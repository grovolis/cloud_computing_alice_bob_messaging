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
		
		TransactionItem partitionKey = new TransactionItem(t_id);
		DynamoDBQueryExpression<TransactionItem> queryExpression = new DynamoDBQueryExpression<TransactionItem>().withHashKeyValues(partitionKey);
		List<TransactionItem> itemList = this.mapper.query(TransactionItem.class, queryExpression);
		
		return (itemList.size() == 0) ? null : itemList.get(0);
	}

	@Override
	public TransactionItem insert(TransactionItem item) {
		try {
			this.mapper.save(item);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return item;
	}

	@Override
	public TransactionItem delete(Object id) {
		if (!(id instanceof String)) {
			throw new IllegalArgumentException("Id must be String type!!");
		}
		
		String t_id = (String)id;
		TransactionItem partitionKey = new TransactionItem(t_id);
		
		try {
			this.mapper.delete(partitionKey);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return partitionKey;
	}

}
