package top.wboss.stomp;


import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import android.util.Log;

import top.wboss.stomp.common.Ack;
import top.wboss.stomp.common.StompCommand;
import top.wboss.stomp.common.StompException;
import top.wboss.stomp.common.StompFrame;
import top.wboss.stomp.websocket.client.WebSocketClient;
import top.wboss.stomp.websocket.handshake.ServerHandshake;

public abstract class WSStompClient {
	
	private static final int SOCKET_MAXSIZE = 1024 * 5;
	
	private URI uri;
	private String username;
	private String password;
	private Long outgoing = 10000L;
	private Long incoming = 10000L;
	
	private Map<String, String> header;

	private Socket socket;
	private WebSocketClient wsSocket;
	private String sessionId;

	private Thread readerThread;
	private Thread pingThread;
	private boolean running;

	/**
	 * constructor
	 * @throws URISyntaxException
	 */
	public WSStompClient() throws URISyntaxException {
		//this("ws://localhost:61613", "abc", "123456");
	}
		
	/**
	 * constructor
	 * @param url
	 * @throws URISyntaxException
	 */
	public WSStompClient(String url, String username, String password) throws URISyntaxException {
		this(new URI(url), username, password);
	}

	/**
	 * constructor
	 * @param address
	 * @param port
	 */
	public WSStompClient(URI uri, String username, String password) {
		this.uri = uri;
		this.username = username;
		this.password = password;
	}
	
	public WSStompClient(URI uri, Map<String, String> header) throws URISyntaxException {
		this.uri = uri;
		this.header = header;
	}
	
	public void createPingThread(){
		// initialize ping thread
		pingThread = new Thread( new Runnable() {
			public void run() {
				ping();
			}
		});
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	// customs handlers
	public abstract void onConnected(Map<String, String> header);
	public abstract void onSubscribe(Map<String, String> header);
	public abstract void onDisconnected();
	public abstract void onStompMessage(String  messageId, String body);
	public abstract void onReceipt(String receiptId);
	public abstract void onStompError(String message, String description);
	public abstract void onCriticalError(Exception e);

	/**
	 * connect() - initialize work with STOMP server
	 * @throws StompException
	 */
	public void wsConnect() throws StompException {
		try {
			// connecting to STOMP server
			if (uri.getScheme().equals("ws") || uri.getScheme().equals("wss")) {
				wsSocket = new WebSocketClient(uri, header) {
					
					@Override
					public void onOpen(ServerHandshake handshakedata) {
						// sending CONNECT command
						StompFrame connectFrame  = new StompFrame(StompCommand.CONNECT);
						connectFrame.header.put("accept-version", StompFrame.VERSIONS_V1_1);
						connectFrame.header.put("heart-beat", outgoing + "," + incoming );
						if( header != null){
							connectFrame.header.putAll(header);
						}else if (username != null) {
							connectFrame.header.put("login", username);
							connectFrame.header.put("passcode", password);
						}
						try {
							stompSend(connectFrame);
						} catch (StompException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						running = true;
					}
					
					@Override
					public void onMessage(String message) {
						String[] msgs = message.split("" + StompFrame.NULL + StompFrame.EOL);
						if(msgs.length > 0){
							for(String msg : msgs){
								if(msg.length() > 0){
									if(msg.charAt(msg.length() -1) == StompFrame.NULL){
										msg += StompFrame.EOL;
									}
									ByteBuffer readBuff = ByteBuffer.wrap(msg.getBytes());
									StompFrame frame = StompFrame.parse(readBuff, 0, readBuff.capacity());
									postMessage(frame);
								}
							}
						}else{
							ByteBuffer readBuff = ByteBuffer.wrap(message.getBytes());
							StompFrame frame = StompFrame.parse(readBuff, 0, readBuff.capacity());
							postMessage(frame);
						}
					}
					
					@Override
					public void onError(Exception ex) {
						onStompError(ex.getMessage(), "");
					}
					
					@Override
					public void onClose(int code, String reason, boolean remote) {
						if(wsSocket!= null && wsSocket.isConnecting()){
							wsSocket.close();
						}
					}
				};
				
				wsSocket.connectBlocking();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
			}
		} catch (Exception e) {
			StompException ex = new StompException("some problem with connection");
			ex.initCause(e);
			throw ex;
		}
	}

	/**
	 * connect() - initialize work with STOMP server
	 * @throws StompException
	 */
	public void connect() throws StompException {
		Thread socketThread = new Thread( new Runnable() {
			public void run() {
				try {
					// connecting to STOMP server
					if (uri.getScheme().equals("tcp")) {
						socket = new Socket(uri.getHost(), uri.getPort());
					} else if (uri.getScheme().equals("tcps")) {
						SocketFactory socketFactory = SSLSocketFactory.getDefault();
					    socket = socketFactory.createSocket(uri.getHost(), uri.getPort());				
					} else {
						throw new StompException("Library is not support this scheme");
					}

					running = true;
					// initialize reader thread
					readerThread = new Thread( new Runnable() {
						public void run() {
							reader();
						}				
					});
					readerThread.start();
					
					// sending CONNECT command
					StompFrame connectFrame  = new StompFrame(StompCommand.CONNECT);
					connectFrame.header.put("accept-version", StompFrame.VERSIONS_V1_1);
					connectFrame.header.put("heart-beat", outgoing + "," + incoming );
					if( header != null){
						connectFrame.header.putAll(header);
					}else if (username != null) {
						connectFrame.header.put("login", username);
						connectFrame.header.put("passcode", password);
					}
					stompSend(connectFrame);
					// wait CONNECTED server command
					synchronized(this) {
						wait(1000);
					}

				} catch (Exception e) {
					StompException ex = new StompException("some problem with connection");
					ex.initCause(e);
				}
			}				
		});
		
		socketThread.start();

	}
	

	/**
	 * reader() - thread for read and parse data from STOMP server
	 */
	private void reader() {	
		int readsize = 0;
		byte[] buf = new byte[SOCKET_MAXSIZE];
		try{
			InputStream in = socket.getInputStream();
			ByteBuffer recvData = null;
			
			while (running && socket != null && socket.isConnected()) {
			
				try {
					readsize = in.read(buf,0, SOCKET_MAXSIZE);
				} catch (IOException e) {
					onDisconnected();
					break;
				}
				
				if(0 >= readsize){
					onDisconnected();
					break;
				}
				// 没有可收数据
				if(null == recvData)
				{
					// 解析用バッファにread結果を設定
					recvData = ByteBuffer.allocate(readsize);
					recvData.put(buf, 0, readsize);
					recvData.position(0);
				}
				// 继续接收数据
				else
				{
					// 扩充解析缓冲
					ByteBuffer readBuff = ByteBuffer.allocate(readsize + recvData.limit());
					// 把上回接收数据放入
					readBuff.put(recvData.array(), 0, recvData.limit());
					// 放入本回接收数据
					readBuff.put(buf, 0, readsize);
					// 解析用缓冲设置
					recvData = readBuff;
				}
				StompFrame frame = null;
				do{
					// parsing raw data to StompFrame format
					frame = StompFrame.parse(recvData, recvData.position(), recvData.capacity());
					
					// 解析长度不足, 等待再接收数据后再处理
					if(frame.remainSize > 0){
						break;
					}else if(frame.parseSize == recvData.capacity()){
						recvData = null;
					}else{ // 继续解析下一条
						// 扩充解析缓冲
						ByteBuffer readBuff = ByteBuffer.allocate(recvData.limit() - frame.parseSize);
						// 把已解析数据除去
						readBuff.put(recvData.array(), frame.parseSize, recvData.limit() - frame.parseSize);
						// 解析用缓冲设置
						recvData = readBuff;
						recvData.position(0);
					}
					postMessage(frame);
				}while(recvData != null && recvData.hasRemaining());
			}
		} catch (IOException e) {
			onCriticalError(e);
			e.printStackTrace();
			onDisconnected();
			return;
		}						
	}
	
	/**
	 * 处理消息
	 * @param frame
	 */
	private void postMessage(StompFrame frame) {
		if(frame.command != null){
			Log.d("WSStompClient", "<<<MESSAGE\r\n" + frame);
			// run handlers
			switch (frame.command) {
				case CONNECTED:
					// unblock connect()
					synchronized(this) { 
						notify(); 
					}
					onConnected(frame.header);

					// run reader thread
					pingThread.start();
					break;
				case DISCONNECTED:
					onDisconnected();
					break;
				case RECEIPT:
					String receiptId = frame.header.get("receipt-id");
					onReceipt(receiptId);
					break;
				case MESSAGE:
					String messageId = frame.header.get("message-id");
					onStompMessage(messageId, frame.body);
					break;						
				case ERROR:
					String message = frame.header.get("message");
					onStompError(message, frame.body);
					if(message.indexOf("Connection to broker closed") > 0){
						onDisconnected();
					}
					break;
				case SUBSCRIBE:
					Log.d("WSStompClient", frame.header.toString());

					onSubscribe(frame.header);
					break;
				default:																				
					break;
			}
		}else if(frame.body != null && (StompFrame.EOL + "").equals(frame.body)){
			Log.d("WSStompClient", "<<<Pong!");
		}
	}

	private void ping() {
		
		try {
			while(running){
				Thread.sleep(outgoing);
				sendPing();
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}				

	protected void sendPing() {
		try {
			if(wsSocket != null && wsSocket.isOpen()){
				wsSocket.send(StompFrame.EOL + "");
			}
			if(socket != null && socket.isConnected()){
				socket.getOutputStream().write(new byte[]{StompFrame.EOL});
			}
			Log.d("WSStompClient", ">>>PING!");
		} catch (Exception e) {
		}

	}

	/**
	 * disconnect() - finalize work with STOMP server
	 */
	public void disconnect(boolean closeFromSrv) {
		running = false;
		
		// sending DISCONNECT command
		StompFrame frame = new StompFrame(StompCommand.DISCONNECT);
		frame.header.put("session", sessionId);
		
		if (wsSocket != null ) {
			try {
				if(!closeFromSrv){
					stompSend(frame);
				}
				// close socket
				wsSocket.close();
			} catch (Exception e) {
			}
			wsSocket = null;
		}
		if(socket != null){
			try {
				if(!closeFromSrv){
					stompSend(frame);
				}
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (StompException e) {
				e.printStackTrace();
			}
			socket = null;
		}
	}

	/**
	 * BEGIN is used to start a transaction. 
	 * @param transaction
	 * @throws StompException
	 * @throws IOException 
	 */
	public void begin(String transaction) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.BEGIN);
		frame.header.put("transaction", transaction);
		stompSend(frame);		
	}

	/**
	 * COMMIT is used to commit a transaction in progress.
	 * @param transaction
	 * @throws StompException
	 * @throws IOException 
	 */
	public void commit(String transaction) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.COMMIT);
		frame.header.put("transaction", transaction);
		stompSend(frame);		
	}

	/**
	 * ABORT is used to roll back a transaction in progress.
	 * @param transaction
	 * @throws StompException
	 * @throws IOException 
	 */
	public void abort(String transaction) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.ABORT);
		frame.header.put("transaction", transaction);
		stompSend(frame);		
	}
	
	/**
	 * The SEND command sends a message to a destination in the messaging system.
	 * @param destination
	 * @param message
	 * @throws StompException
	 * @throws IOException 
	 */
	public void send(String destination, String message) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.SEND);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		frame.body = message;
		stompSend(frame);
	}

	/**
	 * The SEND command sends a message to a destination in the messaging system.
	 * @param destination
	 * @param header
	 * @param message
	 * @throws StompException
	 * @throws IOException 
	 */
	public void send(String destination, Map<String, String> header, String message) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.SEND);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		for(String key: header.keySet()) {
			frame.header.put(key, header.get(key));
		}
		frame.body = message;
		stompSend(frame);
	}
	
	/**
	 * The SUBSCRIBE command is used to register to listen to a given destination.
	 * @param destination
	 * @throws StompException
	 * @throws IOException 
	 */
	public void subscribe(String destination) throws StompException, IOException {
		subscribe(destination, Ack.auto);
	}

	/**
	 * The SUBSCRIBE command is used to register to listen to a given destination.
	 * @param destination
	 * @param ack
	 * @throws StompException
	 * @throws IOException 
	 */
	public void subscribe(String destination, Ack ack) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.SUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		//frame.header.put("id", "sub-0");
		frame.header.put("ack", ack.toString());		
		stompSend(frame);
	}

	/**
	 * The UNSUBSCRIBE command is used to remove an existing subscription
	 * @param destination
	 * @throws StompException
	 * @throws IOException 
	 */
	public void unsubscribe(String destination) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.UNSUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		stompSend(frame);
	}
	
	/**
	 * ACK is used to acknowledge consumption of a message from a subscription using client acknowledgment.
	 * @param messageId
	 * @throws StompException
	 * @throws IOException 
	 */
	public void ack(String messageId) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);		
		stompSend(frame);			
	}
	
	/**
	 * ACK is used to acknowledge consumption of a message from a subscription using client acknowledgment.
	 * @param messageId
	 * @param transaction
	 * @throws StompException
	 * @throws IOException 
	 */
	public void ack(String messageId, String transaction) throws StompException, IOException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);	
		frame.header.put("transaction", transaction);
		stompSend(frame);			
	}	
	
	/**
	 * send - help function for sending any frame to STOMP server
	 * @param frame
	 * @throws StompException
	 * @throws IOException 
	 */
	private synchronized void stompSend(StompFrame frame) throws StompException, IOException {
		if(socket != null && socket.isConnected()){
			socket.getOutputStream().write(frame.getBytes());
		}
		if(wsSocket != null && wsSocket.isOpen()){
			wsSocket.send(frame.getBytes());
		}
	}

	public Map<String, String> getHeader() {
		return header;
	}

	public void setHeader(Map<String, String> header) {
		this.header = header;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * @return the outgoing
	 */
	public Long getOutgoing() {
		return outgoing;
	}

	/**
	 * @param outgoing the outgoing to set
	 */
	public void setOutgoing(Long outgoing) {
		this.outgoing = outgoing;
	}

	/**
	 * @return the incoming
	 */
	public Long getIncoming() {
		return incoming;
	}

	/**
	 * @param incoming the incoming to set
	 */
	public void setIncoming(Long incoming) {
		this.incoming = incoming;
	}

}
