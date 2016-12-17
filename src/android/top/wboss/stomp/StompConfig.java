package top.wboss.stomp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class StompConfig {
	public static final String KEY_URI = "uri";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSCODE = "passcode";
	public static final String KEY_OUTGOING = "outgoing";
	public static final String KEY_SUBSCRIBE = "subscribe";
	public static final String KEY_DESTINATION = "destination";
	public static final String KEY_EXCHANGE = "exchange";
	public static final String KEY_SESSION_ID = "sessionId";
	public static final String KEY_MESSAGE_ID = "messageId";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_HEARDER = "header";
	public static final String KEY_BODY = "body";
	public static final String KEY_RECEIPT_ID = "receiptId";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_EXCEPTION = "exception";
	public static final String KEY_STATE = "state";
	public static final Object KEY_CONNECT = "connect";
	public static final Object KEY_DISCONNECT = "disconnect";

	/**
	 * WebSocket服务器请求地址
	 */
	private URI websocketUri = null;
	/**
	 * Ping 发送的间隔时间
	 */
	private long outgoing = 10000L;
	
	/**
	 * 登录用户名
	 */
	private String login = null;
	
	private String passcode = null;
	
	private static Map<String, String> header = null;
	
	private String thirdParty = null;
	
	public URI getWebsocketUri() {
		return websocketUri;
	}

	public void setWebsocketUri(URI websocketUri) {
		this.websocketUri = websocketUri;
	}
	public void setWebsocketUri(String host) {
		try {
			String websocketUri = host.replace("https://", "wss://").replace("http://", "ws://");
			this.websocketUri = new URI(websocketUri + "/websocket");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getThirdParty() {
		return thirdParty;
	}

	public void setThirdParty(String thirdParty) {
		this.thirdParty = thirdParty;
	}

	public static StompConfig fromJSON(JSONObject json) throws JSONException {
		StompConfig config = new StompConfig();

		
		JSONObject headers = json.optJSONObject("headers");
		if( headers != null){

			config.setWebsocketUri(headers.optString(KEY_URI));
			headers.remove(KEY_URI);

			Iterator<String> keys = headers.keys();
			header = new LinkedHashMap<String, String>();
			while(keys.hasNext()){
				String key = keys.next();
				header.put(key, headers.optString(key));
			}

		}else{
			try {
				config.setWebsocketUri(new URI(json.optString("uri")));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			config.setLogin(json.getString("login"));
			config.setPasscode(json.getString("passcode"));
		}
				
		return config;
	}

	public Map<String, String> getHeader() {
		return header;
	}

	public void setHeader(Map<String, String> header) {
		StompConfig.header = header;
	}

	public String getPasscode() {
		return passcode;
	}

	public void setPasscode(String passcode) {
		this.passcode = passcode;
	}

	public long getOutgoing() {
		return outgoing;
	}

	public void setOutgoing(long outgoing) {
		this.outgoing = outgoing;
	}
}
