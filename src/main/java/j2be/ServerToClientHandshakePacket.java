package j2be;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.netty.buffer.ByteBuf;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;

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
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(readString());
        } catch (ParseException e) {
            throw new IllegalStateException("Invalid signed JWT", e);
        }
        byte[] pubKey = Base64.getDecoder().decode(jwt.getHeader().getX509CertURL().toString());
        try {
            ECDSAVerifier verifier = new ECDSAVerifier((ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(pubKey)));
            jwt.verify(verifier);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (InvalidKeySpecException | JOSEException e) {
            throw new IllegalStateException("Failed to verify salt", e);
        }

        byte[] token;
        try {
            token = Base64.getDecoder().decode(jwt.getJWTClaimsSet().getStringClaim("salt"));
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse JWT claims", e);
        }
    }
}
