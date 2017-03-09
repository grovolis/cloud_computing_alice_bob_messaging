package org.ncl.cloudcomputing.alice;

public class App {
	
	private final static String fileName = "C:\\Users\\alper\\Desktop\\priority.png";
	
	public static void main(String[] args) {
		Alice alice = new Alice();
		
		String docKey = alice.putObjectToBucket(fileName);
    	alice.sendMessageToTTP(docKey);
	}

}
