package j2be;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Type;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

public class LoginPacket extends BEPacket {
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(KeyPair.class, new PublicKeySerializer()).create();
    private static final KeyPair FAKE_KEY;

    static {
        try {
            FAKE_KEY = new ECKeyGenerator(Curve.P_384).generate().toKeyPair();
        } catch (JOSEException e) {
            throw new AssertionError("Earthcomputer is an idiot", e);
        }
    }

    private int protocolVersion;
    private AuthData authData;
    private ClientData clientData;
    private List<KeyPair> chain;
    private KeyPair startKey;

    public LoginPacket(ByteBuf buf) {
        super(buf);
    }

    public LoginPacket(int protocolVersion, AuthData authData, ClientData clientData) {
        this(protocolVersion, authData, clientData, Arrays.asList(FAKE_KEY), FAKE_KEY);
    }

    public LoginPacket(int protocolVersion, AuthData authData, ClientData clientData, List<KeyPair> chain, KeyPair startKey) {
        this.protocolVersion = protocolVersion;
        this.authData = authData;
        this.clientData = clientData;
        this.chain = chain;
        this.startKey = startKey;
    }

    @Override
    protected void serializeExtra() {
        writeInt(protocolVersion);

        ByteBuf contentBuf = Unpooled.buffer();

        List<ChainNode> chainNodes = chain.stream().map(ChainNode::new).collect(Collectors.toList());
        chainNodes.get(chainNodes.size() - 1).extraData = authData;

        List<String> jwsList = new ArrayList<>(chainNodes.size());
        for (int i = 0; i < chainNodes.size(); i++) {
            ChainNode chainNode = chainNodes.get(i);
            String json = GSON.toJson(chainNode);
            JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.ES384), new Payload(json));
            KeyPair key;
            if (i == 0)
                key = startKey;
            else
                key = chainNodes.get(i - 1).identityPublicKey;
            try {
                jws.sign(new ECDSASigner((ECPrivateKey) key.getPrivate()));
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
            jwsList.add(jws.serialize());
        }
        Map<String, List<String>> chainData = ImmutableMap.of("chain", jwsList);
        String chainJson = GSON.toJson(chainData);
        byte[] chainJsonBytes = chainJson.getBytes();
        contentBuf.writeIntLE(chainJsonBytes.length);
        contentBuf.writeBytes(chainJsonBytes);

        String clientJson = GSON.toJson(clientData);
        JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.ES384), new Payload(clientJson));
        try {
            jws.sign(new ECDSASigner((ECPrivateKey) chain.get(chain.size() - 1).getPrivate()));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
        byte[] clientBytes = jws.serialize().getBytes();
        contentBuf.writeIntLE(clientBytes.length);
        contentBuf.writeBytes(clientBytes);

        VarInts.writeUnsignedVarInt(buffer(), contentBuf.writerIndex());
        buffer().writeBytes(contentBuf);
    }

    @Override
    protected void deserializeExtra() {
        unsupported();
    }

    private static class ChainNode {
        KeyPair identityPublicKey;
        AuthData extraData = null;

        public ChainNode(KeyPair identityPublicKey) {
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

    private static class PublicKeySerializer implements JsonSerializer<KeyPair> {

        @Override
        public JsonElement serialize(KeyPair src, Type typeOfSrc, JsonSerializationContext context) {
            X509EncodedKeySpec spec;
            try {
                spec = KeyFactory.getInstance("EC").getKeySpec(src.getPublic(), X509EncodedKeySpec.class);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            byte[] bytes = spec.getEncoded();
            return new JsonPrimitive(Base64.getEncoder().encodeToString(bytes));
        }
    }
}
