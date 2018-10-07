package j2be;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.session.UnumRakNetPeer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WrappedPacketUtils {
	
	private static void addPacketToWrapped(ByteBuf inputPacket, ByteBuf uncompressedPacket) {
		int packetLength = inputPacket.writerIndex();
		VarInts.writeUnsignedVarInt(uncompressedPacket, packetLength);
		uncompressedPacket.writeBytes(inputPacket);
	}
	
	private static ByteBuf emptyWrappedPacket() {
		ByteBuf outputPacket = Unpooled.buffer();
		outputPacket.writeByte(0xfe);
		return outputPacket;
	}

	public static void sendPacket(UnumRakNetPeer peer, BEPacket packet) {
	    sendPackets(peer, Collections.singletonList(packet));
    }

	public static void sendPackets(UnumRakNetPeer peer, List<BEPacket> packets) {
	    sendPackets(peer, packets, 0);
    }

    public static void sendPacket(UnumRakNetPeer peer, BEPacket packet, int channel) {
	    sendPackets(peer, Collections.singletonList(packet), channel);
    }

	public static void sendPackets(UnumRakNetPeer peer, List<BEPacket> packets, int channel) {
	    sendPackets(peer, packets, channel, true);
    }

    public static void sendPacket(UnumRakNetPeer peer, BEPacket packet, int channel, boolean ordered) {
	    sendPackets(peer, Collections.singletonList(packet), channel, ordered);
    }

	public static void sendPackets(UnumRakNetPeer peer, List<BEPacket> packets, int channel, boolean ordered) {
        ByteBuf buf = emptyWrappedPacket();

        ByteBuf uncompressed = PooledByteBufAllocator.DEFAULT.directBuffer();
        for (BEPacket packet : packets) {
            packet.serialize();
            addPacketToWrapped(packet.buffer(), uncompressed);
        }
        CompressionUtils.compress(uncompressed, buf);
        uncompressed.release();

        Packet wrapped = new Packet(buf);
        peer.sendMessage(ordered ? Reliability.RELIABLE_ORDERED : Reliability.RELIABLE, channel, wrapped);
    }

    public static List<BEPacket> unwrapPacket(ByteBuf wrapped) {
	    List<BEPacket> packets = new ArrayList<>();

        ByteBuf uncompressed = PooledByteBufAllocator.DEFAULT.directBuffer();
        CompressionUtils.decompress(wrapped, uncompressed);

        while (uncompressed.isReadable()) {
            int len = VarInts.readUnsignedVarInt(uncompressed);
            ByteBuf packetBuf = uncompressed.readBytes(len);

            BEPacket packet = BEPacket.deserialize(packetBuf);
            packets.add(packet);
        }

        uncompressed.release();

        return packets;
    }

}
