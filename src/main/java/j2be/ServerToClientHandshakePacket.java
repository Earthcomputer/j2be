package j2be;

public class ServerToClientHandshakePacket extends BEPacket {
    @Override
    protected void serializeExtra() {
        // No payload
    }

    @Override
    protected void deserializeExtra() {
        unsupported();
    }
}
