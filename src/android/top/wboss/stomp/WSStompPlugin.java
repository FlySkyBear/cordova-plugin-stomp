package top.wboss.stomp;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import top.wboss.stomp.common.Ack;
import top.wboss.stomp.common.StompException;

import android.app.Activity;
import android.webkit.WebView;

public class WSStompPlugin extends CordovaPlugin {
	
	private Stomp stomp;
	
	
	public WSStompPlugin() {

	}
	
	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {

		final CallbackContext _callbackContext = callbackContext;
		
		if (action.equals("openWSStomp")) {		
			final StompConfig config = StompConfig.fromJSON(args.getJSONObject(0));
			
			_callbackContext.sendPluginResult(getPluginResult());
			
			cordova.getActivity().runOnUiThread(new Runnable() {
				public void run() {					
					try {
						stomp = new Stomp(WSStompPlugin.this, _callbackContext, config);
					} catch (URISyntaxException e) {
						e.printStackTrace();
					} 
					
				}
			});
			
			return true;
		} else if (action.equals("connect")) {		
			cordova.getActivity().runOnUiThread(new Runnable() {
				public void run() {					
					try {
						stomp.connect();
					} catch (StompException e) {
						e.printStackTrace();
					}
				}
			});
			
			return true;
		} else if (action.equals("wsConnect")) {		
			cordova.getActivity().runOnUiThread(new Runnable() {
				public void run() {					
					try {
						stomp.wsConnect();
					} catch (StompException e) {
						e.printStackTrace();
					}
				}
			});
			
			return true;
		} else if (action.equals("subscribe")) {
			final JSONObject jsonObj = args.getJSONObject(0);
			
			cordova.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					try {
						stomp.subscribe(jsonObj.optString(StompConfig.KEY_EXCHANGE), Ack.client);
					} catch(Exception e) {
						_callbackContext.error(e.getMessage());
					}
				}
			});
			
			return true;
		} else if (action.equals("send")) {
			JSONObject container = args.getJSONObject(0);
			JSONObject message = container.optJSONObject(StompConfig.KEY_MESSAGE);
			final String body = message.optString(StompConfig.KEY_BODY);
			final String destination = message.getString(StompConfig.KEY_DESTINATION);
			
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					if (null != stomp) {
						try {
							stomp.send(destination, body);
						} catch (StompException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						};
					}
				}
			});

			return true;
		} else if (action.equals("disconnect")) {

			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(stomp != null){
						stomp.callbackDisconnect(false);
					}
				}
			});
			
			return true;
		}

		callbackContext.error("Invalid action: " + action);
		return false;
	}

	public Activity getActivity() {
		return cordova.getActivity();
	}
	
	public WebView getWebView() {
		return this.getWebView();
	}
	
	
	PluginResult getPluginResult() throws JSONException {
		JSONObject json = new JSONObject();
		
		PluginResult result = new PluginResult(PluginResult.Status.OK, json);
		result.setKeepCallback(true);
		
		return result;
	}
	
	public void onDisconnect() {

	} 
	
}
