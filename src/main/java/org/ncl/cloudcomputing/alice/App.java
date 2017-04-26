package org.ncl.cloudcomputing.alice;

import java.io.File;
import java.util.Scanner;

public class App {
	
	public static void main(String[] args) {
		
		// Alice is instantiated and sends its public key to TTP.
		Alice alice = new Alice();
		alice.registerPublicKey();
		
		Scanner scanner = new Scanner(System.in);
		
		String fileName;
		File f;
		
		// The path of the document that will be sent must be entered by the user.
		do {
			System.out.println("Enter an existing file path to send Bob please: ");
			fileName = scanner.nextLine();
			f = new File(fileName);
		}
		while (!f.isFile());
        
        String[] parts = fileName.split("/");
		String file = parts[parts.length - 1];
		
		// Alice puts the document into the bucket in the cloud.
		String docKey = alice.putObjectToBucket(fileName);
    	alice.sendMessageToTTP(docKey, file);
		
    	// Starts thread to receive messages.
		alice.start();
	}
}
