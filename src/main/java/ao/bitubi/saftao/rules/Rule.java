package ao.bitubi.saftao.rules;

import java.util.List;
import ao.bitubi.saftao.validate.ValidationIssue;

/** Uma regra de negócio da especificação ({@code spec/03-business-rules.md}). */
public interface Rule {

    /** Identificador na especificação, por exemplo {@code R-009}. */
    String id();

    List<ValidationIssue> check(SaftFacts facts);
}
