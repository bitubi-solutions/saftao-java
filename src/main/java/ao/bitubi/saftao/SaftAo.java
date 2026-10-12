package ao.bitubi.saftao;

import ao.bitubi.saftao.internal.SchemaValidator;
import ao.bitubi.saftao.internal.SpecRules;
import ao.bitubi.saftao.rules.FactsExtractor;
import ao.bitubi.saftao.rules.Rule;
import ao.bitubi.saftao.rules.Rules;
import ao.bitubi.saftao.rules.SaftFacts;
import ao.bitubi.saftao.validate.Coverage;
import ao.bitubi.saftao.validate.ValidationIssue;
import ao.bitubi.saftao.validate.ValidationReport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamException;

/**
 * Ponto de entrada: valida um ficheiro SAF-T (AO) 1.01_01.
 *
 * <p>A validação tem dois níveis: o esquema XSD (incluindo unicidade e referências) e as regras de
 * negócio da especificação {@code saftao-spec} que esta versão implementa. O relatório diz, em
 * {@link ValidationReport#getCoverage()}, o que foi verificado. Se o ficheiro não for XML bem
 * formado, só se reportam os erros de esquema.
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
        return new ValidationReport(issues, coverage());
    }

    public static ValidationReport validate(File file) throws IOException {
        return validate(file.toPath());
    }

    /**
     * Valida o conteúdo de um fluxo. Como o ficheiro é lido duas vezes, o fluxo é copiado primeiro
     * para um ficheiro temporário (apagado no fim), em vez de ficar todo em memória. O fluxo é lido
     * até ao fim mas não é fechado.
     */
    public static ValidationReport validate(InputStream xml) throws IOException {
        Path tmp = Files.createTempFile("saftao-", ".xml");
        try {
            Files.copy(xml, tmp, StandardCopyOption.REPLACE_EXISTING);
            return validate(tmp);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Valida o conteúdo em memória. */
    public static ValidationReport validate(byte[] xml) {
        List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        issues.addAll(SchemaValidator.validate(new ByteArrayInputStream(xml)));
        issues.addAll(checkRules(new ByteArrayInputStream(xml)));
        return new ValidationReport(issues, coverage());
    }

    private static Coverage coverage() {
        Set<String> checked = new LinkedHashSet<String>();
        for (Rule rule : Rules.all()) {
            checked.add(rule.id());
        }
        return new Coverage(true, checked, SpecRules.ids());
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
