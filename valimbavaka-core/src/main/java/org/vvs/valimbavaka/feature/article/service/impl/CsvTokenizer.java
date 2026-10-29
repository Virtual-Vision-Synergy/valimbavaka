package org.vvs.valimbavaka.feature.article.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.vvs.valimbavaka.core.exception.BusinessException;

/** Minimal RFC 4180 reader with {@code ;} as separator: quoted fields, doubled quotes, LF / CRLF / CR, blank rows dropped. */
final class CsvTokenizer {

    private static final char SEPARATOR = ';';
    private static final char QUOTE = '"';

    private final String text;
    private final List<List<String>> records = new ArrayList<>();
    private final StringBuilder field = new StringBuilder();
    private List<String> fields = new ArrayList<>();
    private boolean quoted;
    private int position;

    private CsvTokenizer(String text) {
        this.text = text;
    }

    static List<List<String>> tokenize(String text) {
        return new CsvTokenizer(text).run();
    }

    private List<List<String>> run() {
        while (position < text.length()) {
            char c = text.charAt(position++);
            if (quoted) {
                inQuotes(c);
            } else {
                outsideQuotes(c);
            }
        }
        if (quoted) {
            throw new BusinessException("Le fichier CSV contient un guillemet non fermé.");
        }
        endRecord();
        return records;
    }

    private void inQuotes(char c) {
        if (c != QUOTE) {
            field.append(c);
        } else if (position < text.length() && text.charAt(position) == QUOTE) {
            field.append(QUOTE);
            position++;
        } else {
            quoted = false;
        }
    }

    private void outsideQuotes(char c) {
        switch (c) {
            case QUOTE -> quoted = true;
            case SEPARATOR -> endField();
            case '\r' -> {
                skipLineFeed();
                endRecord();
            }
            case '\n' -> endRecord();
            default -> field.append(c);
        }
    }

    private void skipLineFeed() {
        if (position < text.length() && text.charAt(position) == '\n') {
            position++;
        }
    }

    private void endField() {
        fields.add(field.toString().strip());
        field.setLength(0);
    }

    private void endRecord() {
        endField();
        if (fields.stream().anyMatch(value -> !value.isEmpty())) {
            records.add(fields);
        }
        fields = new ArrayList<>();
    }
}
