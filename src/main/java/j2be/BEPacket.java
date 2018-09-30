package j2be;

import com.whirvis.jraknet.Packet;
import io.netty.buffer.ByteBuf;

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
        registerPacket(0x04, ClientToServerHandshakePacket.class);
    }

    protected BEPacket() {}

    protected BEPacket(ByteBuf buf) {
        super(buf);
    }

    public final void serialize() {
        Integer packetId = packetIds.get(getClass());
        if (packetId == null) {
            throw new AssertionError("Tried to send unregistered packet of type " + getClass().getName());
        }
        writeUnsignedByte(packetId);
        serializeExtra();
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
        return packet;
    }

    protected abstract void deserializeExtra();

    protected void unsupported() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    protected void writeBytes(byte[] bytes) {
        buffer().writeBytes(bytes);
    }

}
