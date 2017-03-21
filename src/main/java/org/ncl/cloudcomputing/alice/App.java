package org.ncl.cloudcomputing.alice;

import java.io.File;
import java.util.Scanner;

public class App {
	
	public static void main(String[] args) {
		Alice alice = new Alice();
		alice.registerPublicKey();
		alice.start();
	}
}
