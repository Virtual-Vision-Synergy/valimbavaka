package org.vvs.valimbavaka.core.exception.specifics;

import org.vvs.valimbavaka.core.exception.BaseException;

/** 403 Forbidden - authentifie mais pas autorise. */
public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(message);
    }
}
