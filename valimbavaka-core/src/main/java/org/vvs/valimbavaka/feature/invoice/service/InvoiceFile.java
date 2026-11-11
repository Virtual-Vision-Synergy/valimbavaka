package org.vvs.valimbavaka.feature.invoice.service;

import org.springframework.core.io.Resource;

/** A document ready to be downloaded: the file name proposed to the browser and its bytes. */
public record InvoiceFile(String filename, Resource content) {}
