package solutions.bitubi.saftao;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Valida um ficheiro contra o XSD SAF-T (AO) 1.01_01, incluindo as restrições de identidade
 * ({@code xs:unique} e {@code xs:keyref}), que o motor XML do JDK aplica.
 *
 * <p>Seguro por omissão: não aceita DOCTYPE, não resolve entidades externas e não acede à rede
 * (o {@code xsi:schemaLocation} do ficheiro é ignorado).
 */
final class SchemaValidator {

    static final String XSD_RESOURCE = "/solutions/bitubi/saftao/SAFTAO1.01_01.xsd";

    private static final Schema SCHEMA = loadSchema();

    private SchemaValidator() {
    }

    private static Schema loadSchema() {
        try (InputStream in = SchemaValidator.class.getResourceAsStream(XSD_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("XSD não encontrado no classpath: " + XSD_RESOURCE);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return factory.newSchema(new StreamSource(in));
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Não foi possível carregar o XSD SAF-T (AO)", e);
        }
    }

    static List<ValidationIssue> validate(InputStream xml) {
        final List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        try {
            XMLReader reader = secureReader();
            Validator validator = SCHEMA.newValidator();
            validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    // avisos do motor XML não são problemas do ficheiro
                }

                @Override
                public void error(SAXParseException e) {
                    issues.add(toIssue(e));
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    issues.add(toIssue(e));
                    throw e;
                }
            });
            validator.validate(new SAXSource(reader, new InputSource(xml)));
        } catch (SAXParseException fatal) {
            // já registado pelo ErrorHandler
        } catch (SAXException | IOException | ParserConfigurationException e) {
            issues.add(ValidationIssue.schema("Erro ao ler o ficheiro: " + e.getMessage(), -1, -1));
        }
        return issues;
    }

    private static ValidationIssue toIssue(SAXParseException e) {
        return ValidationIssue.schema(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
    }

    private static XMLReader secureReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeatureIfSupported(spf, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeatureIfSupported(spf, "http://xml.org/sax/features/external-general-entities", false);
        setFeatureIfSupported(spf, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeatureIfSupported(spf, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return spf.newSAXParser().getXMLReader();
    }

    private static void setFeatureIfSupported(SAXParserFactory spf, String feature, boolean value)
            throws ParserConfigurationException, SAXException {
        try {
            spf.setFeature(feature, value);
        } catch (SAXNotRecognizedException | SAXNotSupportedException ignored) {
            // motor XML alternativo: a opção não existe
        }
    }
}
