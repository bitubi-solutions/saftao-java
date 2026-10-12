package ao.bitubi.saftao.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Identificadores das regras de {@code saftao-spec/spec/03-business-rules.md} (para a cobertura). */
public final class SpecRules {

    private static final Set<String> IDS = load();

    private SpecRules() {
    }

    public static Set<String> ids() {
        return IDS;
    }

    private static Set<String> load() {
        Set<String> ids = new TreeSet<String>();
        try (InputStream in = SpecRules.class.getResourceAsStream("spec-rules.txt")) {
            if (in == null) {
                throw new IllegalStateException("spec-rules.txt não encontrado");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    ids.add(line.trim());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro a ler spec-rules.txt", e);
        }
        return Collections.unmodifiableSet(ids);
    }
}
