package org.vvs.valimbavaka.core.exception.specifics;

import org.vvs.valimbavaka.core.exception.BaseException;

/** 401 Unauthorized - non authentifie. */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
