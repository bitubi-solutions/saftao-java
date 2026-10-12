package solutions.bitubi.saftao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solutions.bitubi.saftao.signature.DocumentSigner;
import solutions.bitubi.saftao.signature.PemKeys;
import solutions.bitubi.saftao.signature.SigningMessage;

/** Vectores de {@code saftao-spec/conformance/signature}. */
class SignatureConformanceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static JsonNode messages;
    private static JsonNode chain;

    @BeforeAll
    static void load() throws IOException {
        Path dir = SpecFiles.dir().resolve("conformance").resolve("signature");
        messages = JSON.readTree(Files.readAllBytes(dir.resolve("message-construction.json")));
        chain = JSON.readTree(Files.readAllBytes(dir.resolve("signing-chain.json")));
    }

    @Test
    void signingMessageMatchesVectors() {
        for (JsonNode v : messages.get("messageConstruction")) {
            JsonNode in = v.get("input");
            String actual = SigningMessage.build(
                    in.get("InvoiceDate").asText(), in.get("SystemEntryDate").asText(),
                    in.get("InvoiceNo").asText(), in.get("GrossTotal").asText(),
                    in.get("previousHash").asText());
            assertEquals(v.get("expectedMessage").asText(), actual, v.get("id").asText());
        }
    }

    @Test
    void grossTotalFormatMatchesVectors() {
        for (JsonNode v : messages.get("grossTotalFormat")) {
            assertEquals(v.get("expected").asText(),
                    SigningMessage.formatGrossTotal(new BigDecimal(v.get("input").asText())), v.get("id").asText());
        }
    }

    @Test
    void signingChainProducesExpectedHashes() throws Exception {
        PrivateKey priv = PemKeys.readPrivateKey(chain.get("testPrivateKeyPem").asText());
        PublicKey pub = PemKeys.readPublicKey(chain.get("testPublicKeyPem").asText());
        String previous = "";
        for (JsonNode doc : chain.get("chain")) {
            JsonNode in = doc.get("input");
            assertEquals(previous, in.get("previousHash").asText(), "encadeamento " + doc.get("id").asText());
            String message = SigningMessage.build(in.get("InvoiceDate").asText(), in.get("SystemEntryDate").asText(),
                    in.get("InvoiceNo").asText(), in.get("GrossTotal").asText(), in.get("previousHash").asText());
            assertEquals(doc.get("expectedMessage").asText(), message);

            String hash = DocumentSigner.sign(priv, message);
            assertEquals(doc.get("expectedHash").asText(), hash, doc.get("id").asText());
            assertEquals(172, hash.length());
            assertTrue(DocumentSigner.verify(pub, message, hash));
            previous = hash;
        }
    }

    @Test
    void verifyRejectsTamperedMessageAndGarbage() throws Exception {
        PublicKey pub = PemKeys.readPublicKey(chain.get("testPublicKeyPem").asText());
        JsonNode first = chain.get("chain").get(0);
        String message = first.get("expectedMessage").asText();
        String hash = first.get("expectedHash").asText();
        assertTrue(DocumentSigner.verify(pub, message, hash));
        assertEquals(false, DocumentSigner.verify(pub, message.replace("53.00", "54.00"), hash));
        assertEquals(false, DocumentSigner.verify(pub, message, "isto não é base64!"));
    }

    @Test
    void rejectsMalformedSigningInput() {
        assertThrows(IllegalArgumentException.class,
                () -> SigningMessage.build("18-05-2018", "2018-05-18T11:22:19", "FAC 001/18", "53.00", ""));
        assertThrows(IllegalArgumentException.class,
                () -> SigningMessage.build("2018-05-18", "2018-05-18T11:22", "FAC 001/18", "53.00", ""));
        assertThrows(IllegalArgumentException.class,
                () -> SigningMessage.build("2018-05-18", "2018-05-18T11:22:19", "FAC 001/18", "1,200.00", ""));
        assertThrows(IllegalArgumentException.class, () -> SigningMessage.formatGrossTotal(new BigDecimal("-1")));
    }
}
