package org.vvs.valimbavaka.shared.excel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Polices standard (Helvetica, encodage WinAnsi, rien à embarquer) d'un document PDF : nettoyage des caractères
 * non imprimables, mesure et troncature des textes. Une instance par document.
 */
final class PdfText {

    private static final String ELLIPSIS = "…";
    private static final String REPLACEMENT = "?";
    private static final float TEXT_SIZE = 8;
    private static final float TITLE_SIZE = 14;
    private static final float GLYPH_UNITS = 1000;

    /** Police et taille d'un texte. */
    record Style(PDFont font, float size) {}

    /** Point de départ de la ligne de base d'un texte. */
    record At(float x, float y) {}

    private final Style regular;
    private final Style bold;
    private final Style title;
    private final Map<Integer, Boolean> encodable = new HashMap<>();

    PdfText() {
        PDFont helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        this.regular = new Style(helvetica, TEXT_SIZE);
        this.bold = new Style(helveticaBold, TEXT_SIZE);
        this.title = new Style(helveticaBold, TITLE_SIZE);
    }

    Style regular() {
        return regular;
    }

    Style bold() {
        return bold;
    }

    Style title() {
        return title;
    }

    /** Remplace les blancs (tabulations, retours à la ligne) par une espace et l'inimprimable par « ? ». */
    String sanitize(String value) {
        StringBuilder result = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> result.append(printable(codePoint)));
        return result.toString();
    }

    float width(Style style, String value) throws IOException {
        return style.font().getStringWidth(value) / GLYPH_UNITS * style.size();
    }

    /** Texte tronqué (avec « … ») pour tenir dans {@code maxWidth} ; recherche dichotomique de la longueur. */
    String fit(Style style, String value, float maxWidth) throws IOException {
        if (width(style, value) <= maxWidth) {
            return value;
        }
        int low = 0;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) / 2;
            if (width(style, value.substring(0, middle) + ELLIPSIS) <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low == 0 ? "" : value.substring(0, low) + ELLIPSIS;
    }

    /** Écrit un texte déjà nettoyé par {@link #sanitize(String)}. */
    static void show(PDPageContentStream stream, Style style, String value, At at) throws IOException {
        stream.beginText();
        stream.setFont(style.font(), style.size());
        stream.newLineAtOffset(at.x(), at.y());
        stream.showText(value);
        stream.endText();
    }

    private String printable(int codePoint) {
        if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
            return " ";
        }
        return encodable.computeIfAbsent(codePoint, this::canEncode) ? Character.toString(codePoint) : REPLACEMENT;
    }

    private boolean canEncode(int codePoint) {
        try {
            regular.font().encode(Character.toString(codePoint));
            return true;
        } catch (IllegalArgumentException | IOException e) {
            return false;
        }
    }
}
