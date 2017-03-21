package org.ncl.cloudcomputing.alice;

import java.io.File;
import java.util.Scanner;

public class App {
	
	public static void main(String[] args) {
		
		Alice alice = new Alice();
		alice.registerPublicKey();
		
		Scanner scanner = new Scanner(System.in);
		
		String fileName;
		File f;
		
		do {
			System.out.println("Enter an existing file path to send Bob please: ");
			fileName = scanner.nextLine();
			f = new File(fileName);
		}
		while (!f.isFile());
        
        String[] parts = fileName.split("/");
		String file = parts[parts.length - 1];
		
		String docKey = alice.putObjectToBucket(fileName);
    	alice.sendMessageToTTP(docKey, file);
		
		alice.start();
	}
}
