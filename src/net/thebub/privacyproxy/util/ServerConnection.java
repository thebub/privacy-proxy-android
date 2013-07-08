package net.thebub.privacyproxy.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import com.google.protobuf.ByteString;

import net.thebub.privacyproxy.PrivacyProxyAPI;
import net.thebub.privacyproxy.PrivacyProxyAPI.APICall;
import net.thebub.privacyproxy.PrivacyProxyAPI.APICall.Builder;
import net.thebub.privacyproxy.PrivacyProxyAPI.APIResponse;

/**
 * Connection management class. Implements the singleton pattern.
 * @author dbub
 *
 */
public class ServerConnection {
	
	/**
	 * The address of the server
	 */
//	private static String 	serverAddress 	= "192.168.1.106";
	
	/**
	 * The port of the sever
	 */
	private static Integer 	serverPort 		= 8081;
	
	/**
	 * The singleton instance of the connection manager
	 */
	private static ServerConnection _instance;
	
	/**
	 * The socket for the server connection
	 */
	private Socket serverSocket;
	
	private ServerConnection() {
		super();
	}
	
	/**
	 * The singleton get instance method
	 * @return The instnace of the server connection manager
	 */
	public static synchronized ServerConnection getInstance() {
		// If no inatcne exists, create one
		if(_instance == null) {
			_instance = new ServerConnection();
		}
		
		return _instance;
	}
	
	/**
	 * Connect to the server
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void connect() throws UnknownHostException, IOException {
		this.serverSocket = new Socket(serverAddress, serverPort);
	}
	
	/**
	 * Close the connection to the server
	 * @throws IOException
	 */
	private void disconnect() throws IOException {
		// Close the connection if one is present
		if(serverSocket != null || serverSocket.isConnected()) {
			this.serverSocket.close();
		}
	}
	
	/**
	 * Send an API call to the server
	 * @param request The request object
	 * @return True if sending the request was successfull, false otherwise
	 */
	private boolean send(APICall request) {
		try {
			// If the connection is closed, connect to ther server
			if(serverSocket == null || serverSocket.isClosed() || serverSocket.isOutputShutdown()) {
				this.connect();
			}
			
			// Get the output stream of the socket
			OutputStream os = this.serverSocket.getOutputStream();
			
			// Write the request object delimited over the socket
			request.writeDelimitedTo(os);
			// Flush the socket buffer and send the message
			os.flush();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		
		return true;		
	}
	
	/**
	 * Receive the response from the server
	 * @return The API response object, received from the server
	 */
	private APIResponse receive() {
		APIResponse receivedResponse = null;
		try {
			// Get the inputstream of the socket
        	InputStream is = this.serverSocket.getInputStream();
        	
        	// Receive and parse the delimited api reposne 
        	receivedResponse = APIResponse.parseDelimitedFrom(is);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		                      
        return receivedResponse;
	}
	
	public synchronized APIResponse sendRequest(PrivacyProxyAPI.APICommand command) {
		return this.sendRequest(command, null, null);
	}
	
	public synchronized APIResponse sendRequest(PrivacyProxyAPI.APICommand command, String sessionID) {
		return this.sendRequest(command, sessionID, null);
	}
	
	public synchronized APIResponse sendRequest(PrivacyProxyAPI.APICommand command, ByteString arguments) {
		return this.sendRequest(command, null, arguments);
	}
	
	public synchronized APIResponse sendRequest(PrivacyProxyAPI.APICommand command, String sessionID, ByteString arguments) {
		
		// Instantiate the builder, which will construct a the API request object
		Builder requestBuilder = APICall.newBuilder();
		
		requestBuilder.setCommand(command);
		
		if(sessionID != null)
			requestBuilder.setSessionKey(sessionID);
		
		if(arguments != null)
			requestBuilder.setArguments(arguments);
		
		// Generate the request object
		APICall request = requestBuilder.build();
		
		// Send the request and check for success
		if(!this.send(request)) {
			// Sending was unsuccessful. Return null
			return null;
		}
		
		// Wait and receive the response from the server
		APIResponse receivedResponse = this.receive();
		
		try {
			// Close the connection to the server
			this.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return receivedResponse;
	}
}
