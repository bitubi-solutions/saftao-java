package ao.bitubi.saftao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ao.bitubi.saftao.internal.Messages;
import ao.bitubi.saftao.internal.SpecRules;
import ao.bitubi.saftao.validate.Coverage;
import ao.bitubi.saftao.validate.Severity;
import ao.bitubi.saftao.validate.Source;
import ao.bitubi.saftao.validate.ValidationIssue;
import ao.bitubi.saftao.validate.ValidationReport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** O relatório: gravidade, código, caminho, língua e cobertura. */
class ReportTest {

    private static Path cases() {
        return SpecFiles.dir().resolve("conformance").resolve("cases");
    }

    @Test
    void coverageSaysWhatWasChecked() throws IOException {
        ValidationReport r = SaftAo.validate(cases().resolve("valid-sales-base.xml"));
        Coverage c = r.getCoverage();
        assertTrue(c.isSchemaChecked());
        assertFalse(r.hasErrors());
        assertFalse(c.isComplete(), "enquanto faltam regras, a cobertura não é completa");
        assertTrue(c.getRulesChecked().contains("R-009"));
        assertTrue(c.getRulesNotImplemented().contains("R-001"));
        assertEquals(SpecRules.ids().size(), c.getRulesChecked().size() + c.getRulesNotImplemented().size());
        assertTrue(r.toString().startsWith("SEM ERROS (cobertura parcial"), r.toString());
    }

    @Test
    void ruleIssueHasCodePathAndBothLanguages() throws IOException {
        ValidationReport r = SaftAo.validate(cases().resolve("rule-purchase-count-mismatch.xml"));
        ValidationIssue issue = r.getErrors().get(0);
        assertEquals(Severity.ERROR, issue.getSeverity());
        assertEquals(Source.RULE, issue.getSource());
        assertEquals("R-009", issue.getCode());
        assertEquals("AuditFile/SourceDocuments/PurchaseInvoices/NumberOfEntries", issue.getPath());
        assertTrue(issue.getMessage().contains("não coincide"), issue.getMessage());
        assertTrue(issue.getMessage(Locale.ENGLISH).contains("does not match"), issue.getMessage(Locale.ENGLISH));
        assertEquals(issue.getMessage(), issue.getMessage(Messages.PT_AO));
        assertEquals(issue.getMessage(), issue.getMessage(new Locale("fr")), "língua desconhecida cai em pt_AO");
    }

    @Test
    void schemaIssueHasCodePathAndLine() throws IOException {
        ValidationReport r = SaftAo.validate(cases().resolve("invalid-duplicate-supplier-id.xml"));
        ValidationIssue issue = r.getErrors().get(0);
        assertEquals(Source.SCHEMA, issue.getSource());
        assertEquals("XSD:cvc-identity-constraint", issue.getCode());
        assertNotNull(issue.getDetail());
        assertTrue(issue.getPath().startsWith("AuditFile"), "caminho: " + issue.getPath());
        assertTrue(issue.getMessage(Locale.ENGLISH).startsWith("Schema error"), issue.getMessage(Locale.ENGLISH));
    }

    @Test
    void repeatedElementsGetAnIndexFromTheSecond() throws IOException {
        ValidationReport r = SaftAo.validate(cases().resolve("rule-credit-note-without-reference.xml"));
        for (ValidationIssue i : r.getErrors()) {
            Matcher m = Pattern.compile("Invoice\\[(\\d+)\\]$").matcher(i.getPath());
            if (i.getCode().equals("R-013")) {
                // sem índice = primeira ocorrência; com índice, o número tem de ser >= 2
                assertTrue(i.getPath().endsWith("Invoice") || (m.find() && Integer.parseInt(m.group(1)) >= 2),
                        i.getPath());
            }
        }
    }

    @Test
    void inputStreamAndFileGiveTheSameReport() throws IOException {
        Path file = cases().resolve("rule-purchase-count-mismatch.xml");
        ValidationReport a = SaftAo.validate(file);
        ValidationReport b = SaftAo.validate(new ByteArrayInputStream(Files.readAllBytes(file)));
        ValidationReport c = SaftAo.validate(file.toFile());
        assertEquals(a.getIssues().size(), b.getIssues().size());
        assertEquals(a.getIssues().size(), c.getIssues().size());
        assertEquals(a.getErrors().get(0).getCode(), b.getErrors().get(0).getCode());
    }

    @Test
    void malformedXmlGivesASchemaError() {
        ValidationReport r = SaftAo.validate("<a>".getBytes(StandardCharsets.UTF_8));
        assertTrue(r.hasErrors());
        assertEquals(Source.SCHEMA, r.getErrors().get(0).getSource());
        assertTrue(r.toString().startsWith("COM ERROS"));
    }

    @Test
    void everyImplementedRuleExistsInTheSpecAndHasBothMessages() {
        for (String id : ao.bitubi.saftao.rules.Rules.all().stream().map(ao.bitubi.saftao.rules.Rule::id)
                .toArray(String[]::new)) {
            assertTrue(SpecRules.ids().contains(id), id + " não existe na especificação");
            assertFalse(Messages.format(Messages.PT_AO, "rule." + id).equals("rule." + id), "falta pt_AO: " + id);
            assertFalse(Messages.format(Locale.ENGLISH, "rule." + id).equals(Messages.format(Messages.PT_AO, "rule." + id)),
                    "falta en: " + id);
        }
    }
}
