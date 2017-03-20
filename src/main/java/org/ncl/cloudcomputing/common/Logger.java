package org.ncl.cloudcomputing.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.ncl.cloudcomputing.common.MessageStatus;


public class Logger {
	
	public static void log(String message) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		
		System.out.print(dateFormat.format(date));
		System.out.print(" -- ");
		System.out.print(message);
		
		System.out.println();
	}
	
	public static void logSendMessageOnProcess(MessageStatus status) {
		if (status == MessageStatus.Alice_to_TTP) {
			log("Alice is sending a message to TTP...");
		}
		else if (status == MessageStatus.Bob_to_TTP) {
			log("Bob is sending a message to TTP...");
		}
		else if (status == MessageStatus.TTP_to_Alice) {
			log("TTP is sending a message to Alice...");
		}
		else if (status == MessageStatus.TTP_to_Bob) {
			log("TTP is sending a message to Bob...");
		}
		else if (status == MessageStatus.TTP_to_Bob_doc) {
			log("TTP is sending the document key to Bob...");
		}
		else if (status == MessageStatus.Transaction_Terminate) {
			log("TTP is sending terminate messages to clients");
		}
		else if (status == MessageStatus.Register) {
			log("Public key is being sent");
		}
	}
	
	public static void logSendMessageOnSucceed(MessageStatus status) {
		if (status == MessageStatus.Alice_to_TTP) {
			log("Alice sent the message to TTP");
		}
		else if (status == MessageStatus.Bob_to_TTP) {
			log("Bob sent the message to TTP");
		}
		else if (status == MessageStatus.TTP_to_Alice) {
			log("TTP sent the message to Alice");
		}
		else if (status == MessageStatus.TTP_to_Bob) {
			log("TTP sent the message to Bob");
		}
		else if (status == MessageStatus.TTP_to_Bob_doc) {
			log("TTP sent the document key to Bob");
		}
		else if (status == MessageStatus.Transaction_Terminate) {
			log("TTP sent the terminate messages to clients");
		}
		else if (status == MessageStatus.Register) {
			log("Public key was sent");
		}
	}
	
	public static void logSendMessageOnFail(MessageStatus status) {
		if (status == MessageStatus.Alice_to_TTP) {
			log("Alice could not send the message to TTP");
		}
		else if (status == MessageStatus.Bob_to_TTP) {
			log("Bob could not send the message to TTP");
		}
		else if (status == MessageStatus.TTP_to_Alice) {
			log("TTP could not send the message to Alice");
		}
		else if (status == MessageStatus.TTP_to_Bob) {
			log("TTP could not send the message to Bob");
		}
		else if (status == MessageStatus.TTP_to_Bob_doc) {
			log("TTP could not send the document key to Bob");
		}
		else if (status == MessageStatus.Transaction_Terminate) {
			log("TTP could not send the terminate messages to clients");
		}
		else if (status == MessageStatus.Register) {
			log("Public key could not be sent");
		}
	}
	
	public static void logReceiveMessageOnSucceed(Integer intStatus) {
		if (intStatus == MessageStatus.Alice_to_TTP.getValue()) {
			log("TTP received a message from Alice");
		}
		else if (intStatus == MessageStatus.Bob_to_TTP.getValue()) {
			log("TTP received a message from Bob");
		}
		else if (intStatus == MessageStatus.TTP_to_Alice.getValue()) {
			log("Alice received a message from TTP");
		}
		else if (intStatus == MessageStatus.TTP_to_Bob.getValue()) {
			log("Bob received a message from TTP");
		}
		else if (intStatus == MessageStatus.TTP_to_Bob_doc.getValue()) {
			log("Bob received a document key from TTP");
		}
		else if (intStatus == MessageStatus.Transaction_Terminate.getValue()) {
			log("A termination message was received from TTP");
		}
		else if (intStatus == MessageStatus.Register.getValue()) {
			log("Public key was received from a client");
		}
	}
}
