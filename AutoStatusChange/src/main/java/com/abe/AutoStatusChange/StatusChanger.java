package com.abe.AutoStatusChange;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class StatusChanger {

	public static void main(String[] args) throws Exception {
		
	
		
//		***********************LOGGING CRAP -- BEGIN**********************
		
		Logger logger = Logger.getLogger("StatusChanger");  
	    FileHandler fh;  
 

	        fh = new FileHandler("C:/Users/Abe/Desktop/MyLogFile.log");  
	        logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);  
		
//		***********************LOGGING CRAP -- END**********************
		

		// Here goes the initial information about the sever, username, and
		// password

		System.out.println("Please enter the Server Name");
		Console cs = System.console();
		String server = String.valueOf(cs.readLine());
		System.out.println("Please enter your UserId");
		String userId = String.valueOf(cs.readLine());
		System.out.println("Please enter your password");
		String pw = String.valueOf(cs.readPassword());

		// Creating the initial JSON to be passed to establish the connection

		JsonObject JSON = Json.createObjectBuilder()
				.add("__type", "urn:inin.com:connection:icAuthConnectionRequestSettings")
				.add("applicationName", "Auto Status Changer").add("userID", userId).add("password", pw).build();

		// Create the Jetty HTTPCLient, and accepting all certificates

		SslContextFactory sslContextFactory = new SslContextFactory(true);
		HttpClient httpClient = new HttpClient(sslContextFactory);

		// Let's start the HTTP Client

		httpClient.start();

		// Send the initial Connection Post Request

		ContentResponse res = httpClient.POST("https://" + server + ":8019/icws/connection")
				.header("Accept-Language", "en-us").content(new StringContentProvider(JSON.toString())).send();

		// let's parse the response from the server.
		
		logger.info(res.getContentAsString());

		JsonObject responseJson = parsy(res.getContent());

		// getting the token and the sessionID

		String ININICWSCSR = responseJson.getString("csrfToken");
		String sessionID = responseJson.getString("sessionId");

		// Checking if we got the 201 Created from the server or Nah!

		if (res.getStatus() == 201)
			System.out.println("Connection Established");

		// Retrieving the current status (Get Request)

		String getStatus = "https://" + server + ":8019/icws/" + sessionID + "/status/user-statuses/" + userId;

		Request cr2 = httpClient.newRequest(getStatus).method(HttpMethod.GET).header("ININ-ICWS-CSRF-Token",
				ININICWSCSR);
		ContentResponse rp2 = cr2.send();
		
		logger.info(rp2.getContentAsString());

		// Parsing the response from the server to figure out what's the current
		// status

		System.out.println("You current status is: " + getStatus(rp2));

		// Setting the new status (the PUT request)

		String setStatus = "https://" + server + ":8019/icws/" + sessionID + "/status/user-statuses/" + userId;

		System.out.println("what is your new status?");
		String statusId = String.valueOf(cs.readLine());

		JsonObject JSON2 = Json.createObjectBuilder().add("statusId", statusId).build();

		ContentResponse cr3 = httpClient.newRequest(setStatus).method(HttpMethod.PUT)
				.header("ININ-ICWS-CSRF-Token", ININICWSCSR).content(new StringContentProvider(JSON2.toString()))
				.send();
		logger.info(cr3.getContentAsString());
		
		if(cr3.getStatus()==202)
		System.out.println("Your status has been changed succesfully");


		
		
		
		
		
		

		
		
	}

	public static JsonObject parsy(byte[] bs) throws IOException {

		InputStream st = new ByteArrayInputStream(bs);
		JsonReader jsr = Json.createReader(st);
		JsonObject jo = jsr.readObject();
		st.close();
		return jo;
	}

	public static String getStatus(ContentResponse cr) throws IOException {
		return parsy(cr.getContent()).getString("statusId");
	}
}
