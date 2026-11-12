package org.vvs.valimbavaka.feature.report.service.impl;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;
import org.vvs.valimbavaka.feature.report.repository.ReportSaleRepository;
import org.vvs.valimbavaka.feature.report.service.ReportExportService;
import org.vvs.valimbavaka.feature.report.service.ReportFile;
import org.vvs.valimbavaka.feature.report.service.ReportService;

@Service
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    private final ReportService reportService;
    private final ReportSaleRepository reportSaleRepository;
    private final ReportFileWriter writer;
    private final ReportExportLayout layout;

    ReportExportServiceImpl(
            ReportService reportService,
            ReportSaleRepository reportSaleRepository,
            ReportFileWriter writer,
            ReportExportLayout layout) {
        this.reportService = reportService;
        this.reportSaleRepository = reportSaleRepository;
        this.writer = writer;
        this.layout = layout;
    }

    @Override
    public ReportFile exportXlsx(
            @Nullable ReportGranularity granularity,
            @Nullable LocalDate date,
            @Nullable UUID salesChannelId,
            @Nullable UUID rangeId) {
        var report = reportService.getReport(granularity, date, salesChannelId, rangeId);
        var bytes = writer.xlsx(report, labels(report));
        return new ReportFile(layout.filename(report, "xlsx"), new ByteArrayResource(bytes));
    }

    @Override
    public ReportFile exportPdf(
            @Nullable ReportGranularity granularity,
            @Nullable LocalDate date,
            @Nullable UUID salesChannelId,
            @Nullable UUID rangeId) {
        var report = reportService.getReport(granularity, date, salesChannelId, rangeId);
        var bytes = writer.pdf(report, labels(report));
        return new ReportFile(layout.filename(report, "pdf"), new ByteArrayResource(bytes));
    }

    /** An identifier that matches nothing is printed as it was received: the report itself is then empty. */
    private ReportLabels labels(ReportRDto report) {
        var channelId = report.salesChannelId();
        var rangeId = report.rangeId();
        var channel = channelId == null
                ? null
                : reportSaleRepository.salesChannelLabel(channelId).orElse(channelId.toString());
        var range = rangeId == null
                ? null
                : reportSaleRepository.productRangeLabel(rangeId).orElse(rangeId.toString());
        return new ReportLabels(channel, range);
    }
}
