package ao.bitubi.saftao;

import ao.bitubi.saftao.internal.SchemaValidator;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** O XSD embebido tem de ser idêntico ao de {@code saftao-spec/schema}. */
class BundledSchemaTest {

    @Test
    void bundledXsdMatchesSpec() throws IOException {
        Path spec = SpecFiles.dir().resolve("schema").resolve("SAFTAO1.01_01.xsd");
        try (InputStream in = SchemaValidator.class.getResourceAsStream(SchemaValidator.XSD_RESOURCE)) {
            byte[] bundled = readAll(in);
            assertArrayEquals(Files.readAllBytes(spec), bundled);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
