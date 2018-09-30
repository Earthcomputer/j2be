package j2be;

import java.util.Random;

import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.identifier.MinecraftIdentifier;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;
import com.whirvis.jraknet.session.RakNetClientSession;
import com.whirvis.jraknet.windows.UniversalWindowsProgram;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class TestServer {
	public static void main(String[] args)throws RakNetException{
		// Add loopback exemption for Minecraft
		if (!UniversalWindowsProgram.MINECRAFT.addLoopbackExempt()) {
			System.out.println("Failed to add loopback exemption for Minecraft");
		}
	
		// Create server
		RakNetServer server = new RakNetServer(19132, 10,
				new MinecraftIdentifier("JRakNet Example Server", 282, "1.6.1", 0, 10,
						new Random().nextLong() /* Server broadcast ID */, "New World", "Survival"));
	
		// Add listener
		server.addListener(new RakNetServerListener() {
	
			// Client connected
			@Override
			public void onClientConnect(RakNetClientSession session) {
				System.out.println("Client from address " + session.getAddress() + " has connected to the server");
			}
	
			// Client disconnected
			@Override
			public void onClientDisconnect(RakNetClientSession session, String reason) {
				System.out.println("Client from address " + session.getAddress()
						+ " has disconnected from the server for the reason \"" + reason + "\"");
			}
	
			// Packet received
			@Override
			public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
				System.out.println("Client from address " + session.getAddress() + " sent packet with ID "
						+ RakNet.toHexStringId(packet) + " on channel " + channel);
				
				ByteBuf decompressedPacket = CompressionUtils.decompress(packet.buffer());
				System.out.println(ByteBufUtil.hexDump(decompressedPacket));
				System.out.println("---");
				System.out.println(WrappedPacketUtils.readUnsignedVarInt(decompressedPacket));
					
				
		
			}
	
		});
		server.start();
		
		

	};
}
