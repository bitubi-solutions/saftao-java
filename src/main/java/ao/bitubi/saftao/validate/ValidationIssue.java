package ao.bitubi.saftao.validate;

import ao.bitubi.saftao.internal.Messages;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Um problema encontrado ao validar um ficheiro SAF-T (AO). Imutável.
 *
 * <p>A mensagem é produzida na língua pedida ({@code pt_AO} por defeito, ou {@code en}); o
 * {@link #getCode() código} não muda com a língua.
 */
public final class ValidationIssue {

    private static final Pattern CVC = Pattern.compile("^(cvc-[A-Za-z\\-]+)");

    private final Severity severity;
    private final Source source;
    private final String code;
    private final String messageKey;
    private final String[] args;
    private final String detail;
    private final String path;
    private final int line;
    private final int column;

    private ValidationIssue(Severity severity, Source source, String code, String messageKey, String[] args,
            String detail, String path, int line, int column) {
        this.severity = severity;
        this.source = source;
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
        this.detail = detail;
        this.path = path;
        this.line = line;
        this.column = column;
    }

    /** Erro de esquema, com a mensagem original do motor XML em {@code engineMessage}. */
    public static ValidationIssue schema(String engineMessage, String path, int line, int column) {
        Matcher m = CVC.matcher(engineMessage == null ? "" : engineMessage);
        String text = engineMessage == null ? "" : engineMessage;
        String code;
        if (m.find()) {
            code = "XSD:" + m.group(1); // só a família (cvc-complex-type, cvc-pattern-valid...): igual em todos os JDK
        } else if (text.contains("identity constraint")) {
            code = "XSD:cvc-identity-constraint"; // o JDK 8 não põe o prefixo cvc nestas mensagens
        } else {
            code = "XSD";
        }
        return new ValidationIssue(Severity.ERROR, Source.SCHEMA, code, "schema.error",
                new String[] {engineMessage}, engineMessage, path, line, column);
    }

    /** Violação de uma regra da especificação. A mensagem vem da chave {@code rule.<ruleId>}. */
    public static ValidationIssue rule(String ruleId, String path, int line, int column, String... args) {
        return new ValidationIssue(Severity.ERROR, Source.RULE, ruleId, "rule." + ruleId, args, null, path, line,
                column);
    }

    public Severity getSeverity() {
        return severity;
    }

    public Source getSource() {
        return source;
    }

    /**
     * Código estável: {@code R-009} para regras da especificação; para erros de esquema, a família do
     * erro ({@code XSD:cvc-pattern-valid}, {@code XSD:cvc-identity-constraint}...) ou {@code XSD} se o
     * motor não a indicou.
     */
    public String getCode() {
        return code;
    }

    /** Mensagem em {@code pt_AO}. */
    public String getMessage() {
        return getMessage(Messages.PT_AO);
    }

    /** Mensagem na língua pedida: {@code en}, ou {@code pt_AO} para qualquer outra. */
    public String getMessage(Locale locale) {
        return Messages.format(locale, messageKey, (Object[]) args);
    }

    /** Mensagem original do motor XML (só em erros de esquema; depende da língua da JVM), ou {@code null}. */
    public String getDetail() {
        return detail;
    }

    /**
     * Caminho do elemento no ficheiro, por exemplo
     * {@code AuditFile/SourceDocuments/PurchaseInvoices/Invoice[3]}. O índice só aparece a partir de 2;
     * sem índice é a primeira ocorrência.
     */
    public String getPath() {
        return path;
    }

    /** Linha no ficheiro, ou {@code -1} se desconhecida. */
    public int getLine() {
        return line;
    }

    /** Coluna no ficheiro, ou {@code -1} se desconhecida. */
    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return severity + " " + code + (path == null || path.isEmpty() ? "" : " " + path)
                + (line >= 0 ? " (linha " + line + ")" : "") + ": " + getMessage();
    }
}
