package j2be;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class WrappedPacketUtils {
	
	public static ByteBuf prependLength(ByteBuf inputPacket) {
		ByteBuf outputPacket = Unpooled.buffer();
		int packetLength = inputPacket.writerIndex();
		VarInts.writeUnsignedVarInt(outputPacket, packetLength);
		outputPacket.writeBytes(inputPacket);
		return outputPacket;
	}
	
	public static ByteBuf singlePacketWrap(ByteBuf inputPacket) {
		ByteBuf outputPacket = Unpooled.buffer();
		outputPacket.writeByte(0xfe);
		outputPacket.writeBytes(inputPacket);
		return outputPacket;
		
		
	}
	

}
