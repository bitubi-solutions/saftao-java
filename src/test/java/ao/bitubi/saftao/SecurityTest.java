package ao.bitubi.saftao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ao.bitubi.saftao.validate.ValidationIssue;
import ao.bitubi.saftao.validate.ValidationReport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** O validador não pode aceitar DOCTYPE nem resolver entidades externas. */
class SecurityTest {

    @Test
    void doctypeIsRejected() {
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE AuditFile [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<AuditFile xmlns=\"urn:OECD:StandardAuditFile-Tax:AO_1.01_01\">&x;</AuditFile>";
        ValidationReport r = SaftAo.validate(xml.getBytes(StandardCharsets.UTF_8));
        assertTrue(r.hasErrors());
        for (ValidationIssue i : r.getIssues()) {
            assertFalse(i.getMessage().contains("root:"), "o conteúdo de ficheiros locais não pode vazar: " + i);
        }
        assertTrue(r.getIssues().get(0).getMessage().toLowerCase().contains("doctype"),
                "esperava rejeição do DOCTYPE: " + r.getIssues());
    }

    @Test
    void notXmlIsInvalid() {
        assertTrue(SaftAo.validate("<AuditFile>".getBytes(StandardCharsets.UTF_8)).hasErrors());
    }
}
