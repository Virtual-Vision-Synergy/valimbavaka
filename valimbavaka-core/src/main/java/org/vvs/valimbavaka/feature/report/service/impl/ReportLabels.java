package org.vvs.valimbavaka.feature.report.service.impl;

import org.jspecify.annotations.Nullable;

/** Labels of the filters of a report, printed in its files; {@code null} = no restriction (all channels, ranges). */
record ReportLabels(@Nullable String salesChannel, @Nullable String range) {}
