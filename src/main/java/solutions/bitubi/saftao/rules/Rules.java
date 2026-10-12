package solutions.bitubi.saftao.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import solutions.bitubi.saftao.ValidationIssue;

/**
 * Regras de negócio implementadas nesta versão. Cada uma corresponde a um identificador de
 * {@code spec/03-business-rules.md} do repo {@code saftao-spec}.
 */
public final class Rules {

    private Rules() {
    }

    /** Todas as regras implementadas. */
    public static List<Rule> all() {
        return Collections.unmodifiableList(Arrays.<Rule>asList(
                new TaxNumberFormat(), new PurchaseCount(), new PurchaseCreditNoteReferences()));
    }

    /** R-050: o NIF do cabeçalho não tem espaços nem prefixo de país. */
    static final class TaxNumberFormat implements Rule {
        @Override
        public String id() {
            return "R-050";
        }

        @Override
        public List<ValidationIssue> check(SaftFacts f) {
            List<ValidationIssue> out = new ArrayList<ValidationIssue>();
            String nif = f.taxRegistrationNumber;
            if (nif == null) {
                return out;
            }
            boolean hasSpace = false;
            for (int i = 0; i < nif.length(); i++) {
                if (Character.isWhitespace(nif.charAt(i))) {
                    hasSpace = true;
                    break;
                }
            }
            if (hasSpace || nif.regionMatches(true, 0, "AO", 0, 2)) {
                out.add(ValidationIssue.rule(id(),
                        "TaxRegistrationNumber deve ser o NIF angolano sem espaços e sem prefixo do país",
                        f.taxRegistrationNumberLine, -1));
            }
            return out;
        }
    }

    /** R-009: {@code PurchaseInvoices/NumberOfEntries} é o número de documentos incluídos. */
    static final class PurchaseCount implements Rule {
        @Override
        public String id() {
            return "R-009";
        }

        @Override
        public List<ValidationIssue> check(SaftFacts f) {
            List<ValidationIssue> out = new ArrayList<ValidationIssue>();
            if (f.purchaseNumberOfEntries == null) {
                return out;
            }
            int declared;
            try {
                declared = Integer.parseInt(f.purchaseNumberOfEntries);
            } catch (NumberFormatException e) {
                return out; // o XSD já rejeita valores não inteiros
            }
            if (declared != f.purchaseInvoices.size()) {
                out.add(ValidationIssue.rule(id(),
                        "NumberOfEntries (" + declared + ") não coincide com o número de documentos de compra ("
                                + f.purchaseInvoices.size() + ")",
                        f.purchaseSectionLine, -1));
            }
            return out;
        }
    }

    /** R-013: nota de crédito de compra ({@code NC}) tem de ter {@code References}. */
    static final class PurchaseCreditNoteReferences implements Rule {
        @Override
        public String id() {
            return "R-013";
        }

        @Override
        public List<ValidationIssue> check(SaftFacts f) {
            List<ValidationIssue> out = new ArrayList<ValidationIssue>();
            for (SaftFacts.PurchaseInvoice inv : f.purchaseInvoices) {
                if ("NC".equals(inv.purchaseType) && !inv.hasReferences) {
                    out.add(ValidationIssue.rule(id(),
                            "Nota de crédito de compra" + (inv.invoiceNo == null ? "" : " " + inv.invoiceNo)
                                    + " sem References",
                            inv.line, inv.column));
                }
            }
            return out;
        }
    }
}
