package ao.bitubi.saftao.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Filtro SAX que sabe, em cada momento, o caminho do elemento actual (por exemplo
 * {@code AuditFile/SourceDocuments/SalesInvoices/Invoice[3]/Line[2]}). O índice só aparece a
 * partir de 2.
 */
final class PathTracker extends XMLFilterImpl {

    private static final class Frame {
        final String segment;
        final Map<String, Integer> childCounts = new HashMap<String, Integer>();

        Frame(String segment) {
            this.segment = segment;
        }
    }

    private final Deque<Frame> stack = new ArrayDeque<Frame>();
    private final Map<String, Integer> rootCounts = new HashMap<String, Integer>();

    PathTracker(XMLReader parent) {
        super(parent);
    }

    /** Caminho do elemento actual, ou vazio antes do elemento raiz. */
    String path() {
        StringBuilder sb = new StringBuilder();
        // ArrayDeque.push põe à cabeça: percorre-se do fundo (raiz) para o topo
        java.util.Iterator<Frame> it = stack.descendingIterator();
        while (it.hasNext()) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(it.next().segment);
        }
        return sb.toString();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        String name = localName == null || localName.isEmpty() ? qName : localName;
        Map<String, Integer> counts = stack.isEmpty() ? rootCounts : stack.peek().childCounts;
        Integer previous = counts.get(name);
        int n = previous == null ? 1 : previous + 1;
        counts.put(name, n);
        stack.push(new Frame(n > 1 ? name + "[" + n + "]" : name));
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            super.endElement(uri, localName, qName);
        } finally {
            stack.pop();
        }
    }
}
