package solutions.bitubi.saftao.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Factos extraídos do ficheiro numa única passagem em streaming. As regras de negócio trabalham
 * sobre estes factos, não sobre o XML: ficam simples de testar e o ficheiro não vai todo para memória.
 */
public final class SaftFacts {

    /** Um documento de compra ({@code PurchaseInvoices/Invoice}). */
    public static final class PurchaseInvoice {
        public final int line;
        public final int column;
        public String invoiceNo;
        public String purchaseType;
        public boolean hasReferences;

        PurchaseInvoice(int line, int column) {
            this.line = line;
            this.column = column;
        }
    }

    public String taxRegistrationNumber;
    public int taxRegistrationNumberLine = -1;

    /** Valor de {@code PurchaseInvoices/NumberOfEntries}, ou {@code null} se a secção não existe. */
    public String purchaseNumberOfEntries;
    public int purchaseSectionLine = -1;
    public final List<PurchaseInvoice> purchaseInvoices = new ArrayList<PurchaseInvoice>();
}
