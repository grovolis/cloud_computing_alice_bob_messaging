package org.ncl.cloudcomputing.common;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

public class AWSAuthentication {
	
	public static AWSCredentials getAWSCredentials() {
		return new ProfileCredentialsProvider("default").getCredentials();
	}
}
