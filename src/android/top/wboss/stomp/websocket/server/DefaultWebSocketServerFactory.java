package top.wboss.stomp.websocket.server;

import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

import top.wboss.stomp.websocket.drafts.Draft;
import top.wboss.stomp.websocket.server.WebSocketServer.WebSocketServerFactory;
import top.wboss.stomp.websocket.WebSocketAdapter;
import top.wboss.stomp.websocket.WebSocketImpl;

public class DefaultWebSocketServerFactory implements WebSocketServerFactory {
	@Override
	public WebSocketImpl createWebSocket( WebSocketAdapter a, Draft d, Socket s ) {
		return new WebSocketImpl( a, d );
	}
	@Override
	public WebSocketImpl createWebSocket( WebSocketAdapter a, List<Draft> d, Socket s ) {
		return new WebSocketImpl( a, d );
	}
	@Override
	public SocketChannel wrapChannel( SocketChannel channel, SelectionKey key ) {
		return (SocketChannel) channel;
	}
}