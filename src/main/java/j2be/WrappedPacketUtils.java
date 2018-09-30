package j2be;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;

public class WrappedPacketUtils {
	
	public static int readUnsignedVarInt(ByteBuf buf) {
        int value = 0;
        int i = 0;
        int b;
        while (((b = buf.readByte()) & 0x80) != 0) {
            value |= (b & 0x7F) << i;
            i += 7;
            Preconditions.checkArgument(i <= 35, "Variable length quantity is too long (must be <= 35)");
        }
        return value | (b << i);
    }
	
	
	static long decodeUnsigned(ByteBuf buffer) {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            final byte b = buffer.readByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return (int) result;
            }
        }
        throw new ArithmeticException("Varint was too large");
    }
}
