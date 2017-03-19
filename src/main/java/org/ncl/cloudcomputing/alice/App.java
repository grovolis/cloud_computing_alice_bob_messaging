package org.ncl.cloudcomputing.alice;

public class App {
	
	public static void main(String[] args) {
		String file = "someFile.txt";
		String fileName = System.getProperty("user.dir") + "\\" + file;
		
		Alice alice = new Alice();
		
		String docKey = alice.putObjectToBucket(fileName);
    	alice.sendMessageToTTP(docKey, file);
    	
    	alice.start();
	}

}
