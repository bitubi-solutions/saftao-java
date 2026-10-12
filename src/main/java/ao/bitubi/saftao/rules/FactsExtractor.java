package ao.bitubi.saftao.rules;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Lê o ficheiro em streaming (StAX) e recolhe os {@link SaftFacts}. */
public final class FactsExtractor {

    private static final String HEADER_NIF = "AuditFile/Header/TaxRegistrationNumber";
    private static final String PURCHASES = "AuditFile/SourceDocuments/PurchaseInvoices";
    private static final String PURCHASE_COUNT = PURCHASES + "/NumberOfEntries";
    private static final String PURCHASE_INVOICE = PURCHASES + "/Invoice";
    private static final String PURCHASE_NO = PURCHASE_INVOICE + "/InvoiceNo";
    private static final String PURCHASE_TYPE = PURCHASE_INVOICE + "/PurchaseType";
    private static final String PURCHASE_REFERENCES = PURCHASE_INVOICE + "/References";

    private FactsExtractor() {
    }

    public static SaftFacts extract(InputStream in) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader r = factory.createXMLStreamReader(in);
        try {
            SaftFacts facts = new SaftFacts();
            Deque<String> names = new ArrayDeque<String>();
            SaftFacts.PurchaseInvoice current = null;
            while (r.hasNext()) {
                int event = r.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    names.addLast(r.getLocalName());
                    String path = join(names);
                    int line = r.getLocation().getLineNumber();
                    int col = r.getLocation().getColumnNumber();
                    if (path.equals(HEADER_NIF)) {
                        facts.taxRegistrationNumber = r.getElementText();
                        facts.taxRegistrationNumberLine = line;
                        names.removeLast();
                    } else if (path.equals(PURCHASES)) {
                        facts.purchaseSectionLine = line;
                    } else if (path.equals(PURCHASE_COUNT)) {
                        facts.purchaseNumberOfEntries = r.getElementText().trim();
                        names.removeLast();
                    } else if (path.equals(PURCHASE_INVOICE)) {
                        current = new SaftFacts.PurchaseInvoice(line, col);
                    } else if (current != null && path.equals(PURCHASE_NO)) {
                        current.invoiceNo = r.getElementText();
                        names.removeLast();
                    } else if (current != null && path.equals(PURCHASE_TYPE)) {
                        current.purchaseType = r.getElementText().trim();
                        names.removeLast();
                    } else if (current != null && path.equals(PURCHASE_REFERENCES)) {
                        current.hasReferences = true;
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (current != null && join(names).equals(PURCHASE_INVOICE)) {
                        facts.purchaseInvoices.add(current);
                        current = null;
                    }
                    names.removeLast();
                }
            }
            return facts;
        } finally {
            r.close();
        }
    }

    private static String join(Deque<String> names) {
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(n);
        }
        return sb.toString();
    }
}
