package ao.bitubi.saftao.signature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Constrói a mensagem a assinar do campo {@code Hash} (ver {@code spec/04-signature.md}):
 * {@code Data;DataHoraGravação;NúmeroDocumento;TotalComImpostos;HashAnterior}.
 */
public final class SigningMessage {

    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DATE_TIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");

    private SigningMessage() {
    }

    /**
     * @param documentDate    {@code InvoiceDate} (ou {@code WorkDate}/{@code MovementDate}), {@code AAAA-MM-DD}
     * @param systemEntryDate {@code SystemEntryDate}, {@code AAAA-MM-DDThh:mm:ss}, sem fuso nem milissegundos
     * @param documentNumber  {@code InvoiceNo} (ou {@code DocumentNumber})
     * @param grossTotal      {@code GrossTotal} já formatado (ver {@link #formatGrossTotal(BigDecimal)})
     * @param previousHash    {@code Hash} do documento anterior da mesma série; {@code null} ou vazio no primeiro
     */
    public static String build(String documentDate, String systemEntryDate, String documentNumber,
                               String grossTotal, String previousHash) {
        require(DATE.matcher(nonNull(documentDate, "documentDate")).matches(),
                "documentDate deve ter o formato AAAA-MM-DD: " + documentDate);
        require(DATE_TIME.matcher(nonNull(systemEntryDate, "systemEntryDate")).matches(),
                "systemEntryDate deve ter o formato AAAA-MM-DDThh:mm:ss: " + systemEntryDate);
        nonNull(documentNumber, "documentNumber");
        require(nonNull(grossTotal, "grossTotal").matches("\\d+\\.\\d{2}"),
                "grossTotal deve ter duas casas decimais, ponto decimal e sem separador de milhares: " + grossTotal);
        return documentDate + ";" + systemEntryDate + ";" + documentNumber + ";" + grossTotal + ";"
                + (previousHash == null ? "" : previousHash);
    }

    /**
     * Formata o {@code GrossTotal} para a assinatura: duas casas decimais, ponto como separador,
     * sem separador de milhares. O arredondamento é {@link RoundingMode#HALF_UP}
     * (a legislação diz "arredondado a duas casas decimais" sem indicar o modo).
     *
     * @throws IllegalArgumentException se o valor for negativo (os valores exportam-se em valor absoluto)
     */
    public static String formatGrossTotal(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("GrossTotal não pode ser negativo: " + value);
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nonNull(String v, String name) {
        if (v == null) {
            throw new IllegalArgumentException(name + " é obrigatório");
        }
        return v;
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new IllegalArgumentException(message);
        }
    }
}
