package j2be;

public class ClientToServerHandshakePacket extends BEPacket {
    @Override
    protected void serializeExtra() {
        // No payload
    }

    @Override
    protected void deserializeExtra() {
        unsupported();
    }
}
