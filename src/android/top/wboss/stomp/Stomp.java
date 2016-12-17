package top.wboss.stomp;

import java.net.URISyntaxException;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

public class Stomp extends WSStompClient {
	WSStompPlugin _plugin;
	CallbackContext _callbackContext;
	StompConfig _config;
	
	// Synchronize on quit[0] to avoid teardown-related crashes.
	private Boolean isConnected = false;

	public Stomp(WSStompPlugin plugin, CallbackContext callbackContext, StompConfig config) throws URISyntaxException {
		_plugin = plugin;
		_callbackContext = callbackContext;
		_config = config;
		
		setUri(_config.getWebsocketUri());
		setHeader(_config.getHeader());
		
		super.createPingThread();
	}
	
	void onMessage(JSONObject data) {
		PluginResult result = new PluginResult(PluginResult.Status.OK, data);
		result.setKeepCallback(true);
		_callbackContext.sendPluginResult(result);
	}

	public void callbackDisconnect(boolean disconnectMsg) {
	    synchronized (isConnected) {
	        if (isConnected) {
	        	return;
	        }
	        isConnected = false;
			try {
				JSONObject data = new JSONObject();
				data.put(StompConfig.KEY_STATE, "disconnected");
				onMessage(data);
			} catch (JSONException e) {} 
			 
	        _plugin.onDisconnect();
	    }
	}
	
	public void setConfig(StompConfig config) {
		_config = config;
	}

	@Override
	public void onConnected(Map<String, String> header) {
		isConnected = true;
		String sessionId= header.get("session");
		setSessionId(sessionId);
		JSONObject data = new JSONObject();
		try {
			data.put(StompConfig.KEY_STATE, "connected");
			data.put(StompConfig.KEY_HEARDER, header);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		PluginResult result = new PluginResult(PluginResult.Status.OK, data);
		result.setKeepCallback(true);
		_callbackContext.sendPluginResult(result);
	}

	@Override
	public void onSubscribe(Map<String, String> header) {
		JSONObject data = new JSONObject();
		try {
			data.put(StompConfig.KEY_STATE, "subscribed");
			data.put(StompConfig.KEY_HEARDER, header);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		PluginResult result = new PluginResult(PluginResult.Status.OK, data);
		result.setKeepCallback(true);
		_callbackContext.sendPluginResult(result);
	}

	@Override
	public void onDisconnected() {
		disconnect(true);
		callbackDisconnect(true);
	}

	@Override
	public void onStompMessage(String messageId, String body) {
		JSONObject data = new JSONObject();
		try {
			data.put(StompConfig.KEY_STATE, "onMessage");
			data.put(StompConfig.KEY_MESSAGE_ID, messageId);
			data.put(StompConfig.KEY_MESSAGE, body);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		PluginResult result = new PluginResult(PluginResult.Status.OK, data);
		result.setKeepCallback(true);
		_callbackContext.sendPluginResult(result);
	}

	@Override
	public void onReceipt(String receiptId) {
		JSONObject data = new JSONObject();
		try {
			data.put(StompConfig.KEY_RECEIPT_ID, receiptId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		PluginResult result = new PluginResult(PluginResult.Status.OK, data);
		result.setKeepCallback(true);
		_callbackContext.sendPluginResult(result);
		
	}

	@Override
	public void onStompError(String message, String description) {
		JSONObject data = new JSONObject();
		try {
			data.put(StompConfig.KEY_MESSAGE, message);
			data.put(StompConfig.KEY_DESCRIPTION, description);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		PluginResult result = new PluginResult(PluginResult.Status.ERROR, data);
		result.setKeepCallback(true);
		_callbackContext.sendPluginResult(result);
		
	}

	@Override
	public void onCriticalError(Exception e) {
		JSONObject data = new JSONObject();
		try {
			data.put(StompConfig.KEY_EXCEPTION, e);
		} catch (JSONException je) {
			je.printStackTrace();
		}
		
		PluginResult result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, data);
		result.setKeepCallback(true);
		_callbackContext.sendPluginResult(result);
		
	}


}