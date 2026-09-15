package org.vvs.valimbavaka.common.exception.specifics;

import org.springframework.http.HttpStatus;
import org.vvs.valimbavaka.common.exception.BaseException;

/** 403 Forbidden - authentifie mais pas autorise. */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
