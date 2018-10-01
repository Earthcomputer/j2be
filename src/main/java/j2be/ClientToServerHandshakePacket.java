package j2be;

import io.netty.buffer.ByteBuf;

public class ClientToServerHandshakePacket extends BEPacket {
    public ClientToServerHandshakePacket() {}

    public ClientToServerHandshakePacket(ByteBuf buf) {
        super(buf);
    }

    @Override
    protected void serializeExtra() {
        // No payload
    }

    @Override
    protected void deserializeExtra() {
        unsupported();
    }
}
