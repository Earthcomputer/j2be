package j2be;

import com.whirvis.jraknet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public abstract class BEPacket extends Packet {

    private static final Map<Class<? extends BEPacket>, Integer> packetIds = new IdentityHashMap<>();
    private static final Map<Integer, Class<? extends BEPacket>> packetsById = new HashMap<>();

    private static void registerPacket(int id, Class<? extends BEPacket> clazz) {
        packetIds.put(clazz, id);
        packetsById.put(id, clazz);
    }

    static {
        registerPacket(0x01, LoginPacket.class);
        registerPacket(0x03, ServerToClientHandshakePacket.class);
        registerPacket(0x04, ClientToServerHandshakePacket.class);
    }

    private final ByteBuf buf;

    protected BEPacket() {
        buf = buffer();
    }

    protected BEPacket(ByteBuf buf) {
        super(buf);
        this.buf = buf;
    }

    private boolean isSerialized = false;

    public final void serialize() {
        if (isSerialized) {
            return;
        }

        Integer packetId = packetIds.get(getClass());
        if (packetId == null) {
            throw new AssertionError("Tried to send unregistered packet of type " + getClass().getName());
        }
        writeUnsignedByte(packetId);
        serializeExtra();
        isSerialized = true;
    }

    protected abstract void serializeExtra();

    public static BEPacket deserialize(ByteBuf buf) {
        int packetId = buf.readUnsignedByte();
        Class<? extends BEPacket> clazz = packetsById.get(packetId);
        if (clazz == null) {
            throw new IllegalArgumentException("Packet received with unrecognized ID");
        }
        BEPacket packet;
        try {
            packet = clazz.getConstructor(ByteBuf.class).newInstance(buf);
        } catch (Exception e) {
            throw new AssertionError("Class " + clazz.getName() + " doesn't have a ByteBuf constructor");
        }
        packet.deserializeExtra();
        packet.isSerialized = true;
        buf.setIndex(0, 0);
        return packet;
    }

    protected abstract void deserializeExtra();

    protected void unsupported() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    protected void read(ByteBuf dest) {
        buf.readBytes(dest);
    }

    protected void write(ByteBuf bytes) {
        buf.writeBytes(bytes);
    }

    // Override string methods because why tf would you use the base ones

    @Override
    public String readString() {
        return ByteBufs.readString(buf);
    }

    @Override
    public Packet writeString(String str) {
        ByteBufs.writeString(buf, str);
        return this;
    }

    protected String readLEAsciiString(String str) {
        return ByteBufs.readLEAsciiString(buf);
    }

    protected void writeLEAsciiString(String str) {
        ByteBufs.writeLEAsciiString(buf, str);
    }

    protected int readUnsignedVarInt() {
        return VarInts.readUnsignedVarInt(buf);
    }

    protected void writeUnsignedVarInt(int val) {
        VarInts.writeUnsignedVarInt(buf, val);
    }

}
