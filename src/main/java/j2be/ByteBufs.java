package j2be;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;

public class ByteBufs {

    public static String readString(ByteBuf buf) {
        byte[] bytes = new byte[VarInts.readUnsignedVarInt(buf)];
        buf.readBytes(bytes);
        return new String(bytes, Charsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(Charsets.UTF_8);
        VarInts.writeUnsignedVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readLEAsciiString(ByteBuf buf) {
        byte[] bytes = new byte[buf.readIntLE()];
        buf.readBytes(bytes);
        return new String(bytes, Charsets.US_ASCII);
    }

    public static void writeLEAsciiString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(Charsets.US_ASCII);
        buf.writeIntLE(bytes.length);
        buf.writeBytes(bytes);
    }

}
