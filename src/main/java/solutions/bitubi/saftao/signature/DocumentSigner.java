package solutions.bitubi.saftao.signature;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Assina e verifica o campo {@code Hash} com os parâmetros da AGT (ver {@code spec/04-signature.md}):
 * RSA com SHA-1 e PKCS#1 v1.5, mensagem em UTF-8, assinatura em base64 numa só linha.
 *
 * <p><b>Segurança:</b> SHA-1 e chaves RSA de 1024 bits são fracos hoje. Usar apenas para este campo,
 * porque é o que a AGT define.
 */
public final class DocumentSigner {

    private static final String ALGORITHM = "SHA1withRSA";

    private DocumentSigner() {
    }

    /** @return a assinatura em base64, sem quebras de linha (172 caracteres com uma chave de 1024 bits) */
    public static String sign(PrivateKey key, String message) throws GeneralSecurityException {
        Signature s = Signature.getInstance(ALGORITHM);
        s.initSign(key);
        s.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    /** @return {@code true} se {@code hash} é a assinatura válida de {@code message} */
    public static boolean verify(PublicKey key, String message, String hash) throws GeneralSecurityException {
        Signature s = Signature.getInstance(ALGORITHM);
        s.initVerify(key);
        s.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] signature;
        try {
            signature = Base64.getDecoder().decode(hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return s.verify(signature);
    }
}
