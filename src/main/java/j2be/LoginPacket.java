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
    private static final JWSAlgorithm JWS_ALGORITHM = JWSAlgorithm.ES384;
    private static final Curve KEYGEN_CURVE = Curve.P_384;

    public static KeyPair generateKey() {
        try {
            return new ECKeyGenerator(KEYGEN_CURVE).generate().toKeyPair();
        } catch (JOSEException e) {
            throw new AssertionError(e);
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

    /**
     * Constructor for use with no xbox auth (offline mode)
     */
    public LoginPacket(int protocolVersion, AuthData authData, ClientData clientData) {
        this(protocolVersion, authData, clientData, Arrays.asList(generateKey()), generateKey());
    }

    /**
     * Constructor used to log in with xbox auth
     */
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
        writeChainData(contentBuf);
        writeClientData(contentBuf);

        VarInts.writeUnsignedVarInt(buffer(), contentBuf.writerIndex());
        buffer().writeBytes(contentBuf);
    }

    /**
     * Write chain data containing public key chain and auth data to the buffer
     */
    private void writeChainData(ByteBuf buf) {
        // Convert list of KeyPairs to list of json ChainNodes
        List<ChainNode> chainNodes = chain.stream().map(ChainNode::new).collect(Collectors.toList());
        // Add authData to the end of the chain
        chainNodes.get(chainNodes.size() - 1).extraData = authData;

        // Convert ChainNodes to JWS signed objects
        List<String> jwsList = new ArrayList<>(chainNodes.size());
        for (int i = 0; i < chainNodes.size(); i++) {
            ChainNode chainNode = chainNodes.get(i);

            // convert chain node to json
            String json = GSON.toJson(chainNode);
            // construct jws object with the json as the payload
            JWSObject jws = new JWSObject(new JWSHeader(JWS_ALGORITHM), new Payload(json));

            // use previous key
            KeyPair key;
            if (i == 0)
                key = startKey;
            else
                key = chainNodes.get(i - 1).identityPublicKey;

            // sign the jws object
            try {
                jws.sign(new ECDSASigner((ECPrivateKey) key.getPrivate()));
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }

            // serialize the object and add it to the list
            jwsList.add(jws.serialize());
        }

        // Wrap key chain into {"chain": [...]} json object because that makes sense (totally)
        Map<String, List<String>> chainData = ImmutableMap.of("chain", jwsList);
        String chainJson = GSON.toJson(chainData);

        // Write raw ASCII json to the buffer
        byte[] chainJsonBytes = chainJson.getBytes();
        buf.writeIntLE(chainJsonBytes.length);
        buf.writeBytes(chainJsonBytes);
    }

    /**
     * Write client data to the buffer, containing skin data and other metadata
     */
    private void writeClientData(ByteBuf buf) {
        // Convert clientData to json
        String clientJson = GSON.toJson(clientData);
        // Wrap json in jws object and sign
        JWSObject jws = new JWSObject(new JWSHeader(JWS_ALGORITHM), new Payload(clientJson));
        try {
            jws.sign(new ECDSASigner((ECPrivateKey) chain.get(chain.size() - 1).getPrivate()));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

        // Write raw ASCII serialized jws object to the buffer
        byte[] clientBytes = jws.serialize().getBytes();
        buf.writeIntLE(clientBytes.length);
        buf.writeBytes(clientBytes);
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
