package ao.bitubi.saftao.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Properties;

/**
 * Mensagens em {@code pt_AO} (por defeito) e {@code en}. Não usa {@code ResourceBundle} para
 * funcionar igual no classpath e como módulo automático, e lê os ficheiros em UTF-8.
 */
public final class Messages {

    public static final Locale PT_AO = new Locale("pt", "AO");

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Properties PT = load("messages_pt_AO.properties");
    private static final Properties EN = load("messages_en.properties");

    private Messages() {
    }

    /** Texto da chave, com {@code {0}}, {@code {1}}... substituídos pelos argumentos. */
    public static String format(Locale locale, String key, Object... args) {
        String text = null;
        if (locale != null && "en".equals(locale.getLanguage())) {
            text = EN.getProperty(key);
        }
        if (text == null) {
            text = PT.getProperty(key);
        }
        if (text == null) {
            return key; // chave em falta: devolve-a, nunca falha
        }
        for (int i = 0; args != null && i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    private static Properties load(String name) {
        Properties p = new Properties();
        try (InputStream in = Messages.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Mensagens não encontradas: " + name);
            }
            try (Reader r = new InputStreamReader(in, UTF8)) {
                p.load(r);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro a ler as mensagens: " + name, e);
        }
        return p;
    }
}
