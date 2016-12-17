package top.wboss.stomp.websocket;

import java.net.Socket;
import java.util.List;

import top.wboss.stomp.websocket.drafts.Draft;

public interface WebSocketFactory {
	public WebSocket createWebSocket( WebSocketAdapter a, Draft d, Socket s );
	public WebSocket createWebSocket( WebSocketAdapter a, List<Draft> drafts, Socket s );

}
