package solutions.bitubi.saftao;

/**
 * Um problema encontrado ao validar um ficheiro SAF-T (AO).
 */
public final class ValidationIssue {

    /** Origem do problema. */
    public enum Level {
        /** Viola o XSD (estrutura, tipos, valores, unicidade ou referências do esquema). */
        SCHEMA,
        /** Passa o XSD mas viola uma regra de negócio da especificação. */
        BUSINESS_RULE
    }

    private final Level level;
    private final String ruleId;
    private final String message;
    private final int line;
    private final int column;

    public ValidationIssue(Level level, String ruleId, String message, int line, int column) {
        this.level = level;
        this.ruleId = ruleId;
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public static ValidationIssue schema(String message, int line, int column) {
        return new ValidationIssue(Level.SCHEMA, null, message, line, column);
    }

    public static ValidationIssue rule(String ruleId, String message, int line, int column) {
        return new ValidationIssue(Level.BUSINESS_RULE, ruleId, message, line, column);
    }

    public Level getLevel() {
        return level;
    }

    /** Identificador da regra da especificação (por exemplo {@code R-009}); {@code null} nos erros de esquema. */
    public String getRuleId() {
        return ruleId;
    }

    public String getMessage() {
        return message;
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
        return level + (ruleId == null ? "" : "[" + ruleId + "]")
                + (line >= 0 ? " (linha " + line + ")" : "") + ": " + message;
    }
}
