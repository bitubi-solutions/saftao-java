package solutions.bitubi.saftao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assumptions;

/** Localiza o repo {@code saftao-spec} (propriedade {@code saftao.spec.dir}). */
final class SpecFiles {

    private SpecFiles() {
    }

    static Path dir() {
        String p = System.getProperty("saftao.spec.dir", "../saftao-spec");
        Path dir = Paths.get(p).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir.resolve("conformance"))) {
            throw new IllegalStateException("saftao-spec não encontrado em " + dir
                    + " (use -Dsaftao.spec.dir=...)");
        }
        return dir;
    }
}
