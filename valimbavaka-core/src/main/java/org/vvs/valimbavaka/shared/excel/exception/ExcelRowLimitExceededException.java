package org.vvs.valimbavaka.shared.excel.exception;

import java.io.Serial;

import org.vvs.valimbavaka.core.exception.BusinessException;

/** Export refusé (422) : plus de lignes que {@code vvs.excel.max-rows} ; le client doit affiner ses filtres. */
public class ExcelRowLimitExceededException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExcelRowLimitExceededException(int maxRows) {
        super("Export limité à " + maxRows + " lignes : affinez les filtres");
    }
}
