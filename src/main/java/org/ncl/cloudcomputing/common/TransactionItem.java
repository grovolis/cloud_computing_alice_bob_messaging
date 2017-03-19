package org.ncl.cloudcomputing.common;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="Transactions")
public class TransactionItem {

	public TransactionItem(String id, byte[] signature, String documentKey) {
		this.id = id;
		this.signature = signature;
		this.documentKey = documentKey;
	}
	
	public TransactionItem(String id) {
		this.id = id;
	}
	
	public TransactionItem() { }
	
	private String id;
	private byte[] signature;
	private String documentKey;
	
	@DynamoDBHashKey(attributeName="Id")
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	@DynamoDBAttribute(attributeName="Signature")
	public byte[] getSignature() { return signature; }
	public void setSignature(byte[] signature) { this.signature = signature; }
	
	@DynamoDBAttribute(attributeName="Document_Key")
	public String getDocumentKey() { return documentKey; }
	public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }
}
