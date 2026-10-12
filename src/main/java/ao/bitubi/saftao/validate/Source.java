package ao.bitubi.saftao.validate;

/** De onde vem a verificação que encontrou o problema. */
public enum Source {
    /** O XSD, incluindo unicidade e referências ({@code xs:unique}, {@code xs:keyref}). */
    SCHEMA,
    /** Uma regra de negócio da especificação ({@code R-xxx}). */
    RULE,
    /** Uma observação não confirmada de terceiros ({@code O-xxx}). */
    OBSERVATION
}
