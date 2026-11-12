package org.vvs.valimbavaka.feature.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.dto.ReportComparisonRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRangesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRankingRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportVariationRDto;
import org.vvs.valimbavaka.feature.report.repository.ReportSaleRepository;
import org.vvs.valimbavaka.feature.report.service.ReportService;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);
    private static final ReportSalesRDto SALES = new ReportSalesRDto(BigDecimal.TEN, 1, BigDecimal.TEN);

    @Mock
    private ReportService reportService;

    @Mock
    private ReportSaleRepository reportSaleRepository;

    @Mock
    private ReportFileWriter writer;

    private ReportExportServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ReportExportServiceImpl(reportService, reportSaleRepository, writer, new ReportExportLayout());
    }

    @Test
    void exportXlsx_filters_readsTheReportOfTheScreenAndNamesTheFilters() {
        // Arrange
        var channelId = UUID.randomUUID();
        var rangeId = UUID.randomUUID();
        var report = report(channelId, rangeId);
        when(reportService.getReport(ReportGranularity.DAY, DAY, channelId, rangeId)).thenReturn(report);
        when(reportSaleRepository.salesChannelLabel(channelId)).thenReturn(Optional.of("Boutique"));
        when(reportSaleRepository.productRangeLabel(rangeId)).thenReturn(Optional.of("Soins"));
        when(writer.xlsx(report, new ReportLabels("Boutique", "Soins"))).thenReturn(new byte[] {1, 2, 3});

        // Act
        var file = sut.exportXlsx(ReportGranularity.DAY, DAY, channelId, rangeId);

        // Assert
        assertThat(file.filename()).isEqualTo("rapport-journalier-2026-11-12.xlsx");
        assertThat(((ByteArrayResource) file.content()).getByteArray()).containsExactly(1, 2, 3);
    }

    @Test
    void exportPdf_noFilter_doesNotLookUpAnyLabel() {
        // Arrange
        var report = report(null, null);
        when(reportService.getReport(null, null, null, null)).thenReturn(report);
        when(writer.pdf(report, new ReportLabels(null, null))).thenReturn(new byte[] {1});

        // Act
        var file = sut.exportPdf(null, null, null, null);

        // Assert
        assertThat(file.filename()).isEqualTo("rapport-journalier-2026-11-12.pdf");
        verify(reportSaleRepository, never()).salesChannelLabel(any());
        verify(reportSaleRepository, never()).productRangeLabel(any());
    }

    @Test
    void exportPdf_unknownChannel_printsTheIdentifierReceived() {
        // Arrange
        var channelId = UUID.randomUUID();
        var report = report(channelId, null);
        when(reportService.getReport(any(), any(), eq(channelId), any())).thenReturn(report);
        when(reportSaleRepository.salesChannelLabel(channelId)).thenReturn(Optional.empty());
        when(writer.pdf(report, new ReportLabels(channelId.toString(), null))).thenReturn(new byte[] {1});

        // Act
        var file = sut.exportPdf(null, null, channelId, null);

        // Assert
        assertThat(file.filename()).endsWith(".pdf");
    }

    @Test
    void exportXlsx_periodThatHasNotBegun_failsWithoutWritingAFile() {
        // Arrange
        when(reportService.getReport(any(), any(), any(), any())).thenThrow(new BusinessException("Futur"));

        // Act + Assert
        assertThatThrownBy(() -> sut.exportXlsx(ReportGranularity.YEAR, DAY, null, null))
                .isInstanceOf(BusinessException.class);
        verify(writer, never()).xlsx(any(), any());
    }

    private static ReportRDto report(UUID channelId, UUID rangeId) {
        var comparison = new ReportComparisonRDto(
                DAY.minusDays(1), DAY.minusDays(1), SALES, new ReportVariationRDto(null, null, null));
        return new ReportRDto(
                ReportGranularity.DAY,
                DAY,
                DAY,
                channelId,
                rangeId,
                SALES,
                comparison,
                List.of(),
                new ReportRangesRDto(BigDecimal.ZERO, List.of()),
                new ReportRankingRDto(10, BigDecimal.ZERO, List.of()));
    }
}
