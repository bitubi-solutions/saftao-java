package solutions.bitubi.saftao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado da validação de um ficheiro SAF-T (AO). Imutável.
 */
public final class ValidationReport {

    private final List<ValidationIssue> issues;

    public ValidationReport(List<ValidationIssue> issues) {
        this.issues = Collections.unmodifiableList(new ArrayList<ValidationIssue>(issues));
    }

    /** {@code true} se não há nenhum problema. */
    public boolean isValid() {
        return issues.isEmpty();
    }

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    /** Apenas os problemas do nível indicado. */
    public List<ValidationIssue> getIssues(ValidationIssue.Level level) {
        List<ValidationIssue> out = new ArrayList<ValidationIssue>();
        for (ValidationIssue issue : issues) {
            if (issue.getLevel() == level) {
                out.add(issue);
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return isValid() ? "VÁLIDO" : "INVÁLIDO (" + issues.size() + " problemas)";
    }
}
