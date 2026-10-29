package org.vvs.valimbavaka.feature.article.service.impl;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;

/**
 * Reads the import file: UTF-8 (BOM tolerated), {@code ;} separated, header on the first row. Only the columns of
 * {@link #COLUMNS} are accepted (so an {@code id} or {@code active} column is refused, never silently applied).
 */
@Component
class ArticleCsvParser {

    static final int MAX_ROWS = 1000;

    static final List<String> COLUMNS =
            List.of("code", "name", "barcode", "type", "rangeId", "unit", "cost", "price", "threshold", "supplierId");

    private static final List<String> REQUIRED = List.of("name", "type", "unit", "threshold");
    private static final char BOM = '﻿';

    List<ArticleCsvRecord> parse(byte[] content) {
        var records = CsvTokenizer.tokenize(decode(content));
        if (records.isEmpty()) {
            throw new BusinessException("Le fichier est vide : il faut au moins la ligne d'en-tête.");
        }
        var header = readHeader(records.getFirst());
        var dataRows = records.subList(1, records.size());
        if (dataRows.isEmpty()) {
            throw new BusinessException("Le fichier ne contient aucune ligne d'article.");
        }
        if (dataRows.size() > MAX_ROWS) {
            throw new BusinessException(
                    "Le fichier dépasse %d lignes (%d trouvées).".formatted(MAX_ROWS, dataRows.size()));
        }
        return IntStream.range(0, dataRows.size())
                .mapToObj(index -> toRecord(index + 1, header, dataRows.get(index)))
                .toList();
    }

    private static String decode(byte[] content) {
        try {
            var text = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            return !text.isEmpty() && text.charAt(0) == BOM ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw new BusinessException("Le fichier doit être encodé en UTF-8.");
        }
    }

    private static List<String> readHeader(List<String> raw) {
        Map<String, String> canonical =
                COLUMNS.stream().collect(Collectors.toMap(c -> c.toLowerCase(Locale.ROOT), c -> c));
        var header = raw.stream()
                .map(name -> {
                    var column = canonical.get(name.toLowerCase(Locale.ROOT));
                    if (column == null) {
                        throw new BusinessException("Colonne inconnue : « %s ». Colonnes acceptées : %s."
                                .formatted(name, String.join(", ", COLUMNS)));
                    }
                    return column;
                })
                .toList();
        if (header.stream().distinct().count() != header.size()) {
            throw new BusinessException("Une colonne est répétée dans l'en-tête.");
        }
        var missing =
                REQUIRED.stream().filter(column -> !header.contains(column)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("Colonne obligatoire absente : " + String.join(", ", missing) + ".");
        }
        return header;
    }

    private static ArticleCsvRecord toRecord(int row, List<String> header, List<String> cells) {
        if (cells.size() != header.size()) {
            var error =
                    "Nombre de colonnes incorrect : %d attendues, %d trouvées.".formatted(header.size(), cells.size());
            return new ArticleCsvRecord(row, Map.of(), error);
        }
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            values.put(header.get(i), cells.get(i));
        }
        return new ArticleCsvRecord(row, values, null);
    }
}
