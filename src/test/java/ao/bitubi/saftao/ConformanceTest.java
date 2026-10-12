package ao.bitubi.saftao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ao.bitubi.saftao.validate.Source;
import ao.bitubi.saftao.validate.ValidationIssue;
import ao.bitubi.saftao.validate.ValidationReport;
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

        assertEquals(expectedValid, !report.hasErrors(), "resultado para " + xml.getFileName() + ": " + report.getIssues());
        if (!expectedValid && "schema".equals(level)) {
            assertTrue(hasSource(report, Source.SCHEMA), "esperava um erro de esquema: " + report.getIssues());
        }
        if (!expectedValid && "business-rule".equals(level)) {
            assertTrue(!hasSource(report, Source.SCHEMA), "o ficheiro devia passar o esquema: " + report.getIssues());
            Set<String> found = new HashSet<String>();
            for (ValidationIssue i : report.getErrors()) {
                if (i.getSource() == Source.RULE) {
                    found.add(i.getCode());
                }
            }
            for (JsonNode r : expected.get("rules")) {
                assertTrue(found.contains(r.asText()), "regra esperada " + r.asText() + ", encontradas " + found);
            }
        }
    }

    private static boolean hasSource(ValidationReport report, Source source) {
        for (ValidationIssue i : report.getErrors()) {
            if (i.getSource() == source) {
                return true;
            }
        }
        return false;
    }
}
