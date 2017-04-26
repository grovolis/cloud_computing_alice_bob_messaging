package org.ncl.cloudcomputing.common;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * @author alper
 * Class definition of a table in DynamoDB with Java ORM annotations.
 */
@DynamoDBTable(tableName="Transactions")
public class TransactionItem {

	public TransactionItem(String id, byte[] signature, byte[] documentHash, String documentKey, String filename) {
		this.id = id;
		this.signature = signature;
		this.documentHash = documentHash;
		this.documentKey = documentKey;
		this.filename = filename;
	}
	
	public TransactionItem(String id) {
		this.id = id;
	}
	
	public TransactionItem() { }
	
	private String id;
	private byte[] signature;
	private byte[] documentHash;
	private String documentKey;
	private String filename;
	
	@DynamoDBHashKey(attributeName="Id")
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	@DynamoDBAttribute(attributeName="Signature")
	public byte[] getSignature() { return signature; }
	public void setSignature(byte[] signature) { this.signature = signature; }
	
	@DynamoDBAttribute(attributeName="Document_Hash")
	public byte[] getDocumentHash() { return documentHash; }
	public void setDocumentHash(byte[] documentHash) { this.documentHash = documentHash; }
	
	@DynamoDBAttribute(attributeName="Document_Key")
	public String getDocumentKey() { return documentKey; }
	public void setDocumentKey(String documentKey) { this.documentKey = documentKey; }

	@DynamoDBAttribute(attributeName="Filename")
	public String getFilename() { return filename; }
	public void setFilename(String filename) { this.filename = filename; }
}
