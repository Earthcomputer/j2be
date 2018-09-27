package j2be;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import io.netty.buffer.ByteBuf;

import java.util.*;
import java.util.stream.Collectors;

public class LoginPacket extends BEPacket {
    private static final Gson GSON = new Gson();

    private int protocolVersion;
    private AuthData authData;
    private ClientData clientData;
    private List<String> chain;
    private String startKey;

    public LoginPacket(ByteBuf buf) {
        super(buf);
    }

    public LoginPacket(int protocolVersion, AuthData authData, ClientData clientData) {
        this(protocolVersion, authData, clientData, Arrays.asList("thisKeyIsFake"), "thisKeyIsFake");
    }

    public LoginPacket(int protocolVersion, AuthData authData, ClientData clientData, List<String> chain, String startKey) {
        this.protocolVersion = protocolVersion;
        this.authData = authData;
        this.clientData = clientData;
        this.chain = chain;
        this.startKey = startKey;
    }

    @Override
    protected void serializeExtra() {
        writeInt(protocolVersion);

        List<ChainNode> chainNodes = chain.stream().map(ChainNode::new).collect(Collectors.toList());
        chainNodes.get(chainNodes.size() - 1).extraData = authData;

        List<String> jwsList = new ArrayList<>(chainNodes.size());
        for (int i = 0; i < chainNodes.size(); i++) {
            ChainNode chainNode = chainNodes.get(i);
            String json = GSON.toJson(chainNode);
            JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(json));
            String key;
            if (i == 0)
                key = startKey;
            else
                key = chainNodes.get(i - 1).identityPublicKey;
            try {
                jws.sign(new MACSigner(key));
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
            jwsList.add(jws.serialize());
        }
        Map<String, List<String>> chainData = ImmutableMap.of("chain", jwsList);
        String chainJson = GSON.toJson(chainData);
        byte[] chainJsonEncoded = Base64.getEncoder().encode(chainJson.getBytes());
        writeIntLE(chainJsonEncoded.length);
        writeBytes(chainJsonEncoded);

        String clientJson = GSON.toJson(clientData);
        JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(clientJson));
        try {
            jws.sign(new MACSigner(chain.get(chain.size() - 1)));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        byte[] clientEncoded = Base64.getEncoder().encode(jws.serialize().getBytes());
        writeIntLE(clientEncoded.length);
        writeBytes(clientEncoded);
    }

    @Override
    protected void deserializeExtra() {
        unsupported();
    }

    private static class ChainNode {
        String identityPublicKey;
        AuthData extraData = null;

        public ChainNode(String identityPublicKey) {
            this.identityPublicKey = identityPublicKey;
        }
    }

    public static class AuthData {
        private String displayName;
        private UUID identity;
        @SerializedName("XUID")
        private String xuid;

        public AuthData(String displayName) {
            this(displayName, UUID.randomUUID(), null);
        }

        public AuthData(String displayName, UUID identity, String xuid) {
            this.displayName = displayName;
            this.identity = identity;
            this.xuid = xuid;
        }
    }

    public static class ClientData {
        @SerializedName("GameVersion")
        private String gameVersion;
        @SerializedName("LanguageCode")
        private String languageCode;
        @SerializedName("ServerAddress")
        private String serverAddress;

        public ClientData(String gameVersion, String languageCode, String serverAddress) {
            this.gameVersion = gameVersion;
            this.languageCode = languageCode;
            this.serverAddress = serverAddress;
        }
    }
}
