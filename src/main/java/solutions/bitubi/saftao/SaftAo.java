package solutions.bitubi.saftao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import solutions.bitubi.saftao.rules.FactsExtractor;
import solutions.bitubi.saftao.rules.Rule;
import solutions.bitubi.saftao.rules.Rules;
import solutions.bitubi.saftao.rules.SaftFacts;

/**
 * Ponto de entrada: valida um ficheiro SAF-T (AO) 1.01_01.
 *
 * <p>A validação tem dois níveis: o esquema XSD (incluindo unicidade e referências) e as regras de
 * negócio da especificação {@code saftao-spec} que esta versão implementa. Se o ficheiro não for XML
 * bem formado, só se reportam os erros de esquema.
 */
public final class SaftAo {

    private SaftAo() {
    }

    /** Valida o ficheiro. O ficheiro é lido duas vezes, em streaming. */
    public static ValidationReport validate(Path file) throws IOException {
        List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        try (InputStream in = Files.newInputStream(file)) {
            issues.addAll(SchemaValidator.validate(in));
        }
        try (InputStream in = Files.newInputStream(file)) {
            issues.addAll(checkRules(in));
        }
        return new ValidationReport(issues);
    }

    /** Valida o conteúdo em memória. */
    public static ValidationReport validate(byte[] xml) {
        List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        issues.addAll(SchemaValidator.validate(new ByteArrayInputStream(xml)));
        issues.addAll(checkRules(new ByteArrayInputStream(xml)));
        return new ValidationReport(issues);
    }

    private static List<ValidationIssue> checkRules(InputStream in) {
        List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        SaftFacts facts;
        try {
            facts = FactsExtractor.extract(in);
        } catch (XMLStreamException e) {
            return issues; // XML mal formado: já reportado pelo esquema
        }
        for (Rule rule : Rules.all()) {
            issues.addAll(rule.check(facts));
        }
        return issues;
    }
}
