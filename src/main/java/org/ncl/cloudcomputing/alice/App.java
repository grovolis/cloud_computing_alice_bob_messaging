package org.ncl.cloudcomputing.alice;

public class App {
	
	private final static String fileName = System.getProperty("user.dir") + "\\someFile.txt";
	
	public static void main(String[] args) {
		Alice alice = new Alice();
		
		String docKey = alice.putObjectToBucket(fileName);
    	alice.sendMessageToTTP(docKey);
    	
    	//alice.start();
	}

}
