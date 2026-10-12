package ao.bitubi.saftao.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import ao.bitubi.saftao.validate.ValidationIssue;

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
                out.add(ValidationIssue.rule(id(), "AuditFile/Header/TaxRegistrationNumber",
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
                out.add(ValidationIssue.rule(id(), "AuditFile/SourceDocuments/PurchaseInvoices/NumberOfEntries",
                        f.purchaseSectionLine, -1, String.valueOf(declared),
                        String.valueOf(f.purchaseInvoices.size())));
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
            for (int i = 0; i < f.purchaseInvoices.size(); i++) {
                SaftFacts.PurchaseInvoice inv = f.purchaseInvoices.get(i);
                if ("NC".equals(inv.purchaseType) && !inv.hasReferences) {
                    String path = "AuditFile/SourceDocuments/PurchaseInvoices/Invoice" + (i > 0 ? "[" + (i + 1) + "]" : "");
                    out.add(ValidationIssue.rule(id(), path, inv.line, inv.column,
                            inv.invoiceNo == null ? "?" : inv.invoiceNo));
                }
            }
            return out;
        }
    }
}
