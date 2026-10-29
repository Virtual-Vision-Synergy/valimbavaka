package org.vvs.valimbavaka.feature.article.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.core.exception.BusinessException;

class ArticleCsvParserTest {

    private static final String HEADER = "name;type;unit;threshold";

    private final ArticleCsvParser sut = new ArticleCsvParser();

    @Test
    void parse_validFile_returnsRecordsByColumnName() {
        // Arrange
        var csv = HEADER + "\nSavon noir 100 g;FINISHED;PIECE;10\nHuile de ricin;RAW_MATERIAL;LITER;2,5\n";

        // Act
        var records = sut.parse(bytes(csv));

        // Assert
        assertThat(records).hasSize(2);
        assertThat(records.get(0).row()).isEqualTo(1);
        assertThat(records.get(0).get("name")).isEqualTo("Savon noir 100 g");
        assertThat(records.get(1).get("threshold")).isEqualTo("2,5");
    }

    @Test
    void parse_withBomCrLfAndBlankLines_readsFileCorrectly() {
        // Arrange
        var csv = "﻿" + HEADER + "\r\n\r\nSavon;FINISHED;PIECE;10\r\n;;;\r\n";

        // Act
        var records = sut.parse(bytes(csv));

        // Assert
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("name")).isEqualTo("Savon");
    }

    @Test
    void parse_quotedFieldWithSeparatorAndQuotes_keepsFieldWhole() {
        // Arrange
        var csv = HEADER + "\n\"Savon; \"\"noir\"\"\";FINISHED;PIECE;10\n";

        // Act
        var records = sut.parse(bytes(csv));

        // Assert
        assertThat(records.getFirst().get("name")).isEqualTo("Savon; \"noir\"");
    }

    @Test
    void parse_headerInOtherCase_isAccepted() {
        // Arrange
        var csv = "NAME;Type;UNIT;Threshold;SUPPLIERID\nSavon;FINISHED;PIECE;10;\n";

        // Act
        var records = sut.parse(bytes(csv));

        // Assert
        assertThat(records.getFirst().get("name")).isEqualTo("Savon");
        assertThat(records.getFirst().get("supplierId")).isNull();
    }

    @Test
    void parse_unknownColumnActive_throwsBusinessException() {
        // Arrange
        var csv = HEADER + ";active\nSavon;FINISHED;PIECE;10;false\n";

        // Act + Assert
        assertThatThrownBy(() -> sut.parse(bytes(csv)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Colonne inconnue : « active »");
    }

    @Test
    void parse_missingRequiredColumn_throwsBusinessException() {
        // Arrange
        var csv = "name;type;unit\nSavon;FINISHED;PIECE\n";

        // Act + Assert
        assertThatThrownBy(() -> sut.parse(bytes(csv)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Colonne obligatoire absente : threshold.");
    }

    @Test
    void parse_repeatedColumn_throwsBusinessException() {
        // Arrange
        var csv = HEADER + ";name\nSavon;FINISHED;PIECE;10;Autre\n";

        // Act + Assert
        assertThatThrownBy(() -> sut.parse(bytes(csv)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Une colonne est répétée dans l'en-tête.");
    }

    @Test
    void parse_rowWithWrongColumnCount_returnsRecordWithError() {
        // Arrange
        var csv = HEADER + "\nSavon;FINISHED;PIECE\n";

        // Act
        var records = sut.parse(bytes(csv));

        // Assert
        assertThat(records.getFirst().error()).isEqualTo("Nombre de colonnes incorrect : 4 attendues, 3 trouvées.");
    }

    @Test
    void parse_emptyFile_throwsBusinessException() {
        // Act + Assert
        assertThatThrownBy(() -> sut.parse(new byte[0]))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le fichier est vide : il faut au moins la ligne d'en-tête.");
    }

    @Test
    void parse_headerOnly_throwsBusinessException() {
        // Act + Assert
        assertThatThrownBy(() -> sut.parse(bytes(HEADER + "\n")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le fichier ne contient aucune ligne d'article.");
    }

    @Test
    void parse_unclosedQuote_throwsBusinessException() {
        // Act + Assert
        assertThatThrownBy(() -> sut.parse(bytes(HEADER + "\n\"Savon;FINISHED;PIECE;10\n")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le fichier CSV contient un guillemet non fermé.");
    }

    @Test
    void parse_notUtf8_throwsBusinessException() {
        // Arrange: « é » in ISO-8859-1 is a lone byte, invalid in UTF-8
        var latin1 = (HEADER + "\nSavon é;FINISHED;PIECE;10\n").getBytes(StandardCharsets.ISO_8859_1);

        // Act + Assert
        assertThatThrownBy(() -> sut.parse(latin1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le fichier doit être encodé en UTF-8.");
    }

    @Test
    void parse_exactlyMaxRows_isAccepted() {
        // Act
        var records = sut.parse(bytes(fileWithRows(ArticleCsvParser.MAX_ROWS)));

        // Assert
        assertThat(records).hasSize(ArticleCsvParser.MAX_ROWS);
    }

    @Test
    void parse_oneRowOverMax_throwsBusinessException() {
        // Act + Assert
        assertThatThrownBy(() -> sut.parse(bytes(fileWithRows(ArticleCsvParser.MAX_ROWS + 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le fichier dépasse 1000 lignes (1001 trouvées).");
    }

    private static String fileWithRows(int rows) {
        var csv = new StringBuilder(HEADER).append('\n');
        for (int i = 1; i <= rows; i++) {
            csv.append("Article ").append(i).append(";RAW_MATERIAL;PIECE;1\n");
        }
        return csv.toString();
    }

    private static byte[] bytes(String csv) {
        return csv.getBytes(StandardCharsets.UTF_8);
    }
}
