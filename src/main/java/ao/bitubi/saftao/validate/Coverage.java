package ao.bitubi.saftao.validate;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * O que foi verificado. Um relatório sem erros só prova o que esta cobertura diz que foi verificado.
 */
public final class Coverage {

    private final boolean schemaChecked;
    private final Set<String> rulesChecked;
    private final Set<String> rulesNotImplemented;

    public Coverage(boolean schemaChecked, Set<String> rulesChecked, Set<String> specRules) {
        this.schemaChecked = schemaChecked;
        this.rulesChecked = Collections.unmodifiableSet(new TreeSet<String>(rulesChecked));
        TreeSet<String> missing = new TreeSet<String>(specRules);
        missing.removeAll(rulesChecked);
        this.rulesNotImplemented = Collections.unmodifiableSet(missing);
    }

    /** Se o ficheiro foi validado contra o XSD. */
    public boolean isSchemaChecked() {
        return schemaChecked;
    }

    /** Regras da especificação que foram verificadas (por exemplo {@code R-009}). */
    public Set<String> getRulesChecked() {
        return rulesChecked;
    }

    /** Regras da especificação que esta versão ainda não implementa. */
    public Set<String> getRulesNotImplemented() {
        return rulesNotImplemented;
    }

    /** {@code true} se o esquema e todas as regras da especificação foram verificados. */
    public boolean isComplete() {
        return schemaChecked && rulesNotImplemented.isEmpty();
    }

    @Override
    public String toString() {
        int total = rulesChecked.size() + rulesNotImplemented.size();
        return "Esquema: " + (schemaChecked ? "sim" : "não") + " · Regras: " + rulesChecked.size() + " de " + total;
    }
}
