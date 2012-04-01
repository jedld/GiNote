package com.dayosoft.utils;

class ClientCredentials {

	/** Value of the "API key" shown under "Simple API Access". */
	public static final String CLIENT_ID = "559626734170.apps.googleusercontent.com";
	public static final String SECRET = "uZEiHyPpwnzwxo9z-3fOC0qS";

	public static void errorIfNotSpecified() {
		if (CLIENT_ID == null) {
			System.err.println("Please enter your API key in "
					+ ClientCredentials.class);
			System.exit(1);
		}
	}
}
