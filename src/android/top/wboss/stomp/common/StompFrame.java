package top.wboss.stomp.common;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * @author yaroslav.gaponov
 *
 */
public class StompFrame {
	public static final char EOL = 0x0A;
	public static final char NULL = 0x00;
	public static final char CR = 0x0D;
	
	// public static final String PING_CHAR = "" + 0x09;
	public static final String VERSIONS_V1_1 = "1.1";
	public static final String CONTENT_LENGTH = "content-length";
	
	public StompCommand command;
	public Map<String, String> header = new HashMap<String, String>();
	public String body;
	// 处理剩下的字节长度
	public int remainSize = 0;
	public int parseSize = 0;

	/**
	 * constructor
	 * 
	 */
	public StompFrame() {
	}

	/**
	 * constructor
	 * 
	 * @param command
	 *            type of frame
	 */
	public StompFrame(StompCommand command) {
		this.command = command;
	}

	public String toString() {
		String frame = (command != null ? this.command.toString() : "") + EOL;
		for (String key : this.header.keySet()) {
			frame += key + ":" + this.header.get(key) + EOL;
		}
		frame += EOL;

		if (this.body != null) {
			frame += this.body;
		}
		
		return frame;
	}

	/**
	 * getBytes convert frame object to array of bytes
	 * 
	 * @return array of bytes
	 */
	public byte[] getBytes() {
		String frame = toString();
		Log.d("WSStompPlugin", ">>>SEND\r\n" + frame);
		frame += NULL;

		return frame.getBytes();
	}

	/**
	 * parse string to frame object
	 * 
	 * @param raw
	 *            frame as string
	 * @return frame object
	 */
	public static StompFrame parse(ByteBuffer buff, int startIndex, int endIndex) {
		StompFrame frame = new StompFrame();
		final int eol = findEndOfMessage(buff, startIndex, endIndex);
	    if (eol != -1) {
	    	frame.parseSize = eol;
	    	int newlen = eol - startIndex;
	    	
	    	if(newlen > 2 && buff.get(newlen - 2) == NULL && buff.get(newlen - 1) == EOL){
	    		newlen = newlen -2;
	    	}
			String raw = new String(buff.array(), startIndex, newlen);
	
			if ((EOL + "").equals(raw)) {
				frame.command = null;
				frame.body = raw;
				return frame;
			}
			String[] commandSections = raw.split("\n\n");
			if (commandSections.length > 0) {
				String commandheaderSections = commandSections[0];
				String[] headerLines = commandheaderSections.split("\n");
	
				int index = 0;
				String commandLine = null;
				while (index < headerLines.length
						&& (commandLine = headerLines[index++]).length() == 0) {
	
				}
				if (commandLine != null) {
					try {
						frame.command = StompCommand.valueOf(commandLine);
					} catch (Exception e) {
						frame.command = null;
						frame.body = commandLine;
						return frame;
					}
				}
	
				for (int i = index; i < headerLines.length; i++) {
					String key = headerLines[i].split(":")[0];
					frame.header.put(key,
							headerLines[i].substring(key.length() + 1));
				}
	
				frame.body = raw.substring(commandheaderSections.length() + 2);
				if(frame.header.containsKey(CONTENT_LENGTH)){
					int bodyLen = Integer.valueOf(frame.header.get(CONTENT_LENGTH));
					if(bodyLen > frame.body.length()){
						frame.remainSize = bodyLen - frame.body.length();
					}
				}
			} else {
				frame.command = null;
				frame.body = raw.substring(0, 0);
			}
	    }else{
	    	frame.remainSize = endIndex - startIndex;
	    }
		return frame;
	}

	/**
	 * Returns the index in the buffer of the end of line found. Returns -1 if
	 * no end of line was found in the buffer.
	 */
	private static int findEndOfMessage(final ByteBuffer buff, int starIndex,
			int endIndex) {

		for (int i = starIndex; i < endIndex; i++) {
			final byte b = buff.get(i);
			if (b == NULL ) {
				if (i < endIndex - 1 && buff.get(i + 1) == EOL) {
					return (i + 2);
				}
				return ++i;
			} else if (endIndex - starIndex == 2 && b == CR
					&& i < endIndex - 1 && buff.get(i + 1) == EOL) {
				return (i + 2); // \r\n
			} else if (endIndex - starIndex < 4 && buff.get(endIndex - 1) == EOL) {
				return endIndex;
			}
		}
		return -1; // Not found.
	}

}
