package org.ncl.cloudcomputing.common;

import org.ncl.cloudcomputing.common.MessageStatus;


public class Logger {
	
	public static void logSendMessageOnProcess(MessageStatus status) {
		if (status == MessageStatus.Alice_to_TTP) {
			System.out.println("Alice is sending a message to TTP...");
		}
		else if (status == MessageStatus.Bob_to_TTP) {
			System.out.println("Bob is sending a message to TTP...");
		}
		else if (status == MessageStatus.TTP_to_Alice) {
			System.out.println("TTP is sending a message to Alice...");
		}
		else if (status == MessageStatus.TTP_to_Bob) {
			System.out.println("TTP is sending a message to Bob...");
		}
		else if (status == MessageStatus.TTP_to_Bob_doc) {
			System.out.println("TTP is sending the document key to Bob...");
		}
	}
	
	public static void logSendMessageOnSucceed(MessageStatus status) {
		if (status == MessageStatus.Alice_to_TTP) {
			System.out.println("Alice sent the message to TTP");
		}
		else if (status == MessageStatus.Bob_to_TTP) {
			System.out.println("Bob sent the message to TTP");
		}
		else if (status == MessageStatus.TTP_to_Alice) {
			System.out.println("TTP sent the message to Alice");
		}
		else if (status == MessageStatus.TTP_to_Bob) {
			System.out.println("TTP sent the message to Bob");
		}
		else if (status == MessageStatus.TTP_to_Bob_doc) {
			System.out.println("TTP sent the document key to Bob");
		}
	}
	
	public static void logSendMessageOnFail(MessageStatus status) {
		if (status == MessageStatus.Alice_to_TTP) {
			System.out.println("Alice could not send the message to TTP");
		}
		else if (status == MessageStatus.Bob_to_TTP) {
			System.out.println("Bob could not send the message to TTP");
		}
		else if (status == MessageStatus.TTP_to_Alice) {
			System.out.println("TTP could not send the message to Alice");
		}
		else if (status == MessageStatus.TTP_to_Bob) {
			System.out.println("TTP could not send the message to Bob");
		}
		else if (status == MessageStatus.TTP_to_Bob_doc) {
			System.out.println("TTP could not send the document key to Bob");
		}
	}
	
	public static void logReceiveMessageOnSucceed(Integer intStatus) {
		if (intStatus == MessageStatus.Alice_to_TTP.getValue()) {
			System.out.println("TTP received a message from Alice");
		}
		else if (intStatus == MessageStatus.Bob_to_TTP.getValue()) {
			System.out.println("TTP received a message from Bob");
		}
		else if (intStatus == MessageStatus.TTP_to_Alice.getValue()) {
			System.out.println("Alice received a message from TTP");
		}
		else if (intStatus == MessageStatus.TTP_to_Bob.getValue()) {
			System.out.println("Bob received a message from TTP");
		}
		else if (intStatus == MessageStatus.TTP_to_Bob_doc.getValue()) {
			System.out.println("Bob received a document key from TTP");
		}
	}
}
