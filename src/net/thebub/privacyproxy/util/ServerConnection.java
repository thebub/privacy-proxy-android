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

public class ServerConnection {
	
	private static String 	serverAddress 	= "192.168.1.106";
	private static Integer 	serverPort 		= 8081;
	
	private static ServerConnection _instance;
	
	private Socket serverSocket;
	
	private ServerConnection() {
		super();
	}
	
	public static synchronized ServerConnection getInstance() {
		if(_instance == null) {
			_instance = new ServerConnection();
		}
		
		return _instance;
	}
	
	private void connect() throws UnknownHostException, IOException {
		this.serverSocket = new Socket(serverAddress, serverPort);
	}
	
	private void disconnect() throws IOException {
		if(serverSocket != null || serverSocket.isConnected()) {
			this.serverSocket.close();
		}
	}
	
	private boolean send(APICall request) {
		try {		
			if(serverSocket == null || serverSocket.isClosed() || serverSocket.isOutputShutdown()) {
				this.connect();
			}
						
			OutputStream os = this.serverSocket.getOutputStream();
				
			request.writeDelimitedTo(os);			
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
	
	private APIResponse receive() {
		APIResponse response;
		try {		
        	InputStream is = this.serverSocket.getInputStream();
        	
        	response = APIResponse.parseDelimitedFrom(is);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
                      
        return response;
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
		
		Builder requestBuilder = APICall.newBuilder();
		
		requestBuilder.setCommand(command);
		
		if(sessionID != null)
			requestBuilder.setSessionKey(sessionID);
		
		if(arguments != null)
			requestBuilder.setArguments(arguments);
		
		APICall request = requestBuilder.build();
		
		if(!this.send(request)) {
			return null;
		}
		
		return this.receive();
	}
	
	private APIResponse receiveResponse() {
		return this.receive();
	}

}
