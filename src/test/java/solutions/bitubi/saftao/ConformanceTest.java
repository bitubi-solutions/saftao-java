package solutions.bitubi.saftao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Corre todos os casos de {@code saftao-spec/conformance/cases}. */
class ConformanceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TestFactory
    Stream<DynamicTest> cases() throws IOException {
        Path cases = SpecFiles.dir().resolve("conformance").resolve("cases");
        List<Path> xmls = new ArrayList<Path>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(cases, "*.xml")) {
            for (Path p : ds) {
                xmls.add(p);
            }
        }
        Collections.sort(xmls);
        assertTrue(xmls.size() >= 10, "esperava pelo menos 10 casos, encontrei " + xmls.size());
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final Path xml : xmls) {
            String name = xml.getFileName().toString().replaceAll("\\.xml$", "");
            tests.add(DynamicTest.dynamicTest(name, () -> check(xml, cases.resolve(name + ".expected.json"))));
        }
        return tests.stream();
    }

    private static void check(Path xml, Path expectedFile) throws IOException {
        JsonNode expected = JSON.readTree(expectedFile.toFile());
        boolean expectedValid = expected.get("valid").asBoolean();
        String level = expected.get("level").asText();

        ValidationReport report = SaftAo.validate(xml);

        assertEquals(expectedValid, report.isValid(), "resultado para " + xml.getFileName() + ": " + report.getIssues());
        if (!expectedValid && "schema".equals(level)) {
            assertTrue(!report.getIssues(ValidationIssue.Level.SCHEMA).isEmpty(),
                    "esperava um erro de esquema: " + report.getIssues());
        }
        if (!expectedValid && "business-rule".equals(level)) {
            assertTrue(report.getIssues(ValidationIssue.Level.SCHEMA).isEmpty(),
                    "o ficheiro devia passar o esquema: " + report.getIssues());
            Set<String> found = new HashSet<String>();
            for (ValidationIssue i : report.getIssues(ValidationIssue.Level.BUSINESS_RULE)) {
                found.add(i.getRuleId());
            }
            for (JsonNode r : expected.get("rules")) {
                assertTrue(found.contains(r.asText()), "regra esperada " + r.asText() + ", encontradas " + found);
            }
        }
    }
}
