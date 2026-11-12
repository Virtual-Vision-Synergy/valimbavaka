package org.vvs.valimbavaka.feature.report.service;

import org.springframework.core.io.Resource;

/** A report file ready to be downloaded (RPT-06): the file name proposed to the browser and its bytes. */
public record ReportFile(String filename, Resource content) {}
