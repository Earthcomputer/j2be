package j2be;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

// TODO: remove this class when we move to Minecraft
public class VarInts {

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

    public static void writeUnsignedVarInt(ByteBuf buf, int val) {
        while ((val & 0xFFFFFF80) != 0L) {
            buf.writeByte((val & 0x7F) | 0x80);
            val >>>= 7;
        }
        buf.writeByte(val & 0x7F);
    }
}
