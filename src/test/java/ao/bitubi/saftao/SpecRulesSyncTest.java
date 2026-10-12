package ao.bitubi.saftao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ao.bitubi.saftao.internal.SpecRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** A lista de regras embebida tem de coincidir com a especificação (senão a cobertura engana). */
class SpecRulesSyncTest {

    @Test
    void embeddedListMatchesSpec() throws IOException {
        String md = new String(Files.readAllBytes(SpecFiles.dir().resolve("spec").resolve("03-business-rules.md")),
                StandardCharsets.UTF_8);
        Set<String> inSpec = new TreeSet<String>();
        Matcher m = Pattern.compile("\\bR-[0-9]{3}\\b").matcher(md);
        while (m.find()) {
            inSpec.add(m.group());
        }
        assertEquals(inSpec, SpecRules.ids(),
                "actualizar src/main/resources/ao/bitubi/saftao/internal/spec-rules.txt a partir da especificação");
    }
}
