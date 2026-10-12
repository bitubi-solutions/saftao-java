package ao.bitubi.saftao.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Resultado da validação de um ficheiro SAF-T (AO). Imutável.
 *
 * <p>{@link #hasErrors()} só diz se as verificações <em>feitas</em> encontraram erros. O que foi
 * feito está em {@link #getCoverage()}: sem erros e com cobertura parcial não é o mesmo que válido.
 */
public final class ValidationReport {

    private static final Comparator<ValidationIssue> BY_LINE = new Comparator<ValidationIssue>() {
        @Override
        public int compare(ValidationIssue a, ValidationIssue b) {
            int la = a.getLine() < 0 ? Integer.MAX_VALUE : a.getLine();
            int lb = b.getLine() < 0 ? Integer.MAX_VALUE : b.getLine();
            return la < lb ? -1 : (la == lb ? 0 : 1);
        }
    };

    private final List<ValidationIssue> issues;
    private final Coverage coverage;

    public ValidationReport(List<ValidationIssue> issues, Coverage coverage) {
        List<ValidationIssue> sorted = new ArrayList<ValidationIssue>(issues);
        Collections.sort(sorted, BY_LINE); // estável: mantém a ordem de verificação em linhas iguais
        this.issues = Collections.unmodifiableList(sorted);
        this.coverage = coverage;
    }

    /** {@code true} se há pelo menos um problema de gravidade {@link Severity#ERROR}. */
    public boolean hasErrors() {
        return !getErrors().isEmpty();
    }

    public List<ValidationIssue> getErrors() {
        return filter(Severity.ERROR);
    }

    public List<ValidationIssue> getWarnings() {
        return filter(Severity.WARNING);
    }

    /** Todos os problemas, por ordem de linha no ficheiro (os sem linha no fim). */
    public List<ValidationIssue> getIssues() {
        return issues;
    }

    /** O que foi e não foi verificado. */
    public Coverage getCoverage() {
        return coverage;
    }

    private List<ValidationIssue> filter(Severity severity) {
        List<ValidationIssue> out = new ArrayList<ValidationIssue>();
        for (ValidationIssue issue : issues) {
            if (issue.getSeverity() == severity) {
                out.add(issue);
            }
        }
        return out;
    }

    @Override
    public String toString() {
        if (hasErrors()) {
            return "COM ERROS (" + getErrors().size() + " erros, " + getWarnings().size() + " avisos)";
        }
        return "SEM ERROS (" + (coverage.isComplete() ? "cobertura completa" : "cobertura parcial: " + coverage) + ")";
    }
}
