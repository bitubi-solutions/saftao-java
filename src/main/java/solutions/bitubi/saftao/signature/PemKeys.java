package solutions.bitubi.saftao.signature;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Lê chaves RSA em PEM. O JDK só lê PKCS#8; o OpenSSL (e o exemplo da AGT) produz também
 * PKCS#1 ({@code BEGIN RSA PRIVATE KEY}), por isso esse formato é convertido aqui.
 */
public final class PemKeys {

    // AlgorithmIdentifier rsaEncryption (OID 1.2.840.113549.1.1.1, parâmetros NULL)
    private static final byte[] RSA_ALGORITHM = {
        0x30, 0x0D, 0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00
    };

    private PemKeys() {
    }

    public static PrivateKey readPrivateKey(String pem) throws GeneralSecurityException {
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            byte[] pkcs1 = der(pem, "RSA PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(wrapPkcs8(pkcs1)));
        }
        if (pem.contains("BEGIN PRIVATE KEY")) {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der(pem, "PRIVATE KEY")));
        }
        throw new GeneralSecurityException("Chave privada PEM não reconhecida (esperado RSA PRIVATE KEY ou PRIVATE KEY)");
    }

    public static PublicKey readPublicKey(String pem) throws GeneralSecurityException {
        if (!pem.contains("BEGIN PUBLIC KEY")) {
            throw new GeneralSecurityException("Chave pública PEM não reconhecida (esperado PUBLIC KEY)");
        }
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der(pem, "PUBLIC KEY")));
    }

    private static byte[] der(String pem, String label) {
        String body = pem.replaceAll("-----BEGIN " + label + "-----", "")
                .replaceAll("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    /** PrivateKeyInfo ::= SEQUENCE { version 0, algorithm rsaEncryption, privateKey OCTET STRING (PKCS#1) }. */
    private static byte[] wrapPkcs8(byte[] pkcs1) {
        ByteArrayOutputStream inner = new ByteArrayOutputStream();
        inner.write(0x02);
        inner.write(0x01);
        inner.write(0x00);
        inner.write(RSA_ALGORITHM, 0, RSA_ALGORITHM.length);
        inner.write(0x04);
        writeLength(inner, pkcs1.length);
        inner.write(pkcs1, 0, pkcs1.length);
        byte[] content = inner.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x30);
        writeLength(out, content.length);
        out.write(content, 0, content.length);
        return out.toByteArray();
    }

    private static void writeLength(ByteArrayOutputStream out, int length) {
        if (length < 0x80) {
            out.write(length);
        } else if (length <= 0xFF) {
            out.write(0x81);
            out.write(length);
        } else if (length <= 0xFFFF) {
            out.write(0x82);
            out.write(length >> 8);
            out.write(length & 0xFF);
        } else {
            out.write(0x83);
            out.write(length >> 16);
            out.write((length >> 8) & 0xFF);
            out.write(length & 0xFF);
        }
    }
}
