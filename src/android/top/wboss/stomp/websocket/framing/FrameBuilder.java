package top.wboss.stomp.websocket.framing;

import java.nio.ByteBuffer;

import top.wboss.stomp.websocket.exceptions.InvalidDataException;


public interface FrameBuilder extends Framedata {

	public abstract void setFin( boolean fin );

	public abstract void setOptcode( Opcode optcode );

	public abstract void setPayload( ByteBuffer payload ) throws InvalidDataException;

	public abstract void setTransferemasked( boolean transferemasked );

}