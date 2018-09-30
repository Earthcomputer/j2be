package j2be;

import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import com.whirvis.jraknet.RakNetException;
import com.whirvis.jraknet.client.RakNetClient;
import com.whirvis.jraknet.client.RakNetClientListener;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.session.RakNetServerSession;

public class TestClient {

    public static void main(String[] cmdLineArgs) {
        // Input ip and port
        String ip;
        int port;
        if (cmdLineArgs.length > 0) {
            // Get IP and port from command line
            ip = cmdLineArgs[0];
            if (cmdLineArgs.length > 1) {
                try {
                    port = Integer.parseInt(cmdLineArgs[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port");
                    return;
                }
            } else {
                port = 19132;
            }
        } else {
            // Get IP and port from GUI prompt
            ip = JOptionPane.showInputDialog(null, "Enter IP plox:");
            if (ip == null)
                return;
            port = 19132;
        }
        
        createClient(ip, port);
    }
    
    private static void createClient(String ip, int port) {
        // Create client
        RakNetClient client = new RakNetClient();
                
        // Add listener
        client.addListener(new RakNetClientListener() {

            // Server connected
            @Override
            public void onConnect(RakNetServerSession session) {
                System.out.println("Successfully connected to server with address " + session.getAddress());
                //client.disconnect();
                ClientToServerHandshakePacket packet = new ClientToServerHandshakePacket();
                packet.serialize();
                session.sendMessage(Reliability.RELIABLE, packet);
                /*
                LoginPacket.AuthData authData = new LoginPacket.AuthData("TestUsername");
                LoginPacket.ClientData clientData = new LoginPacket.ClientData("1.6.1", "en_US", ip + ":" + port);
                LoginPacket packet = new LoginPacket(282, authData, clientData);
                packet.serialize();
                session.sendMessage(Reliability.RELIABLE, packet);
                */
            }

            // Server disconnected
            @Override
            public void onDisconnect(RakNetServerSession session, String reason) {
                System.out.println("Successfully disconnected from server with address " + session.getAddress()
                        + " for the reason \"" + reason + "\"");
                client.shutdown();
            }

        });
        
        try {
            client.connect(ip, port);
        } catch (UnknownHostException e) {
            System.err.println("Yo giving me fake IPs: " + ip + ":" + port);
        } catch (RakNetException e) {
            e.printStackTrace();
        }
    }

}
