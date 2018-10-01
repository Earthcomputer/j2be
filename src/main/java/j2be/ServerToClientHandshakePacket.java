package j2be;

import io.netty.buffer.ByteBuf;

public class ServerToClientHandshakePacket extends BEPacket {
    public ServerToClientHandshakePacket() {}

    public ServerToClientHandshakePacket(ByteBuf buf) {
        super(buf);
    }

    @Override
    protected void serializeExtra() {
        unsupported();
    }

    @Override
    protected void deserializeExtra() {
        // Empty payload
    }
}
