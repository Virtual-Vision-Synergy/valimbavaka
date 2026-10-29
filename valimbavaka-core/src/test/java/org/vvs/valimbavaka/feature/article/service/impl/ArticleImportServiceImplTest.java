package org.vvs.valimbavaka.feature.article.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.article.domain.ImportConflictMode;
import org.vvs.valimbavaka.feature.article.domain.ImportRowStatus;
import org.vvs.valimbavaka.feature.article.dto.ArticleImportRowRDto;

@ExtendWith(MockitoExtension.class)
class ArticleImportServiceImplTest {

    private static final String HEADER = "code;name;barcode;type;unit;threshold\n";

    @Mock
    private ArticleImportRowProcessor rowProcessor;

    private ArticleImportServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ArticleImportServiceImpl(new ArticleCsvParser(), rowProcessor);
    }

    @Test
    void importArticles_mixedRows_countsEachStatus() {
        // Arrange
        when(rowProcessor.process(any(), eq(ImportConflictMode.SKIP)))
                .thenReturn(result(1, ImportRowStatus.CREATED))
                .thenReturn(result(2, ImportRowStatus.CONFLICT))
                .thenReturn(result(3, ImportRowStatus.INVALID));
        var file = csv(";A;;RAW_MATERIAL;PIECE;1\n;B;;RAW_MATERIAL;PIECE;1\n;C;;RAW_MATERIAL;PIECE;1\n");

        // Act
        var report = sut.importArticles(file, ImportConflictMode.SKIP, Set.of());

        // Assert
        assertThat(report.created()).isEqualTo(1);
        assertThat(report.conflicts()).isEqualTo(1);
        assertThat(report.invalid()).isEqualTo(1);
        assertThat(report.rows()).hasSize(3);
    }

    @Test
    void importArticles_barcodeRepeatedInFile_rejectsSecondRowWithoutProcessingIt() {
        // Arrange
        when(rowProcessor.process(any(), any())).thenReturn(result(1, ImportRowStatus.CREATED));
        var file = csv(";A;3760001230014;RAW_MATERIAL;PIECE;1\n;B;3760001230014;RAW_MATERIAL;PIECE;1\n");

        // Act
        var report = sut.importArticles(file, ImportConflictMode.OVERWRITE, Set.of());

        // Assert
        assertThat(report.rows().get(1).status()).isEqualTo(ImportRowStatus.CONFLICT);
        assertThat(report.rows().get(1).message())
                .isEqualTo("Doublon dans le fichier : barcode « 3760001230014 » déjà présent à la ligne 1.");
        verify(rowProcessor).process(any(), any());
    }

    @Test
    void importArticles_codeRepeatedInFile_rejectsSecondRow() {
        // Arrange
        when(rowProcessor.process(any(), any())).thenReturn(result(1, ImportRowStatus.CREATED));
        var file = csv("ART-0500;A;;RAW_MATERIAL;PIECE;1\nART-0500;B;;RAW_MATERIAL;PIECE;1\n");

        // Act
        var report = sut.importArticles(file, ImportConflictMode.SKIP, Set.of());

        // Assert
        assertThat(report.rows().get(1).status()).isEqualTo(ImportRowStatus.CONFLICT);
        assertThat(report.rows().get(1).message()).startsWith("Doublon dans le fichier : code « ART-0500 »");
    }

    @Test
    void importArticles_onlyRows_processesOnlyTheListedRows() {
        // Arrange
        when(rowProcessor.process(any(), eq(ImportConflictMode.OVERWRITE)))
                .thenReturn(result(2, ImportRowStatus.UPDATED));
        var file = csv(";A;;RAW_MATERIAL;PIECE;1\n;B;;RAW_MATERIAL;PIECE;1\n;C;;RAW_MATERIAL;PIECE;1\n");

        // Act
        var report = sut.importArticles(file, ImportConflictMode.OVERWRITE, Set.of(2));

        // Assert
        assertThat(report.rows()).hasSize(1);
        assertThat(report.updated()).isEqualTo(1);
    }

    @Test
    void importArticles_databaseRefusesRow_reportsConflictAndGoesOn() {
        // Arrange
        when(rowProcessor.process(any(), any()))
                .thenThrow(new DataIntegrityViolationException("uq_articles_barcode"))
                .thenReturn(result(2, ImportRowStatus.CREATED));
        var file = csv(";A;;RAW_MATERIAL;PIECE;1\n;B;;RAW_MATERIAL;PIECE;1\n");

        // Act
        var report = sut.importArticles(file, ImportConflictMode.SKIP, Set.of());

        // Assert
        assertThat(report.conflicts()).isEqualTo(1);
        assertThat(report.created()).isEqualTo(1);
    }

    @Test
    void importArticles_invalidFile_throwsBeforeWritingAnything() {
        // Arrange
        var file = new MockMultipartFile("file", "articles.csv", "text/csv", new byte[0]);

        // Act + Assert
        assertThatThrownBy(() -> sut.importArticles(file, ImportConflictMode.SKIP, Set.of()))
                .isInstanceOf(BusinessException.class);
        verify(rowProcessor, never()).process(any(), any());
    }

    @Test
    void importArticles_unreadableFile_throwsBusinessException() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("disk"));

        // Act + Assert
        assertThatThrownBy(() -> sut.importArticles(file, ImportConflictMode.SKIP, Set.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Le fichier n'a pas pu être lu.");
    }

    private static ArticleImportRowRDto result(int row, ImportRowStatus status) {
        return new ArticleImportRowRDto(row, status, null, null);
    }

    private static MultipartFile csv(String rows) {
        return new MockMultipartFile(
                "file", "articles.csv", "text/csv", (HEADER + rows).getBytes(StandardCharsets.UTF_8));
    }
}
