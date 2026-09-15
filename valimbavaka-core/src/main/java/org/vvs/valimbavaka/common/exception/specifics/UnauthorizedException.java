package org.vvs.valimbavaka.common.exception.specifics;

import org.springframework.http.HttpStatus;
import org.vvs.valimbavaka.common.exception.BaseException;

/** 401 Unauthorized - non authentifie. */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
