package org.vvs.valimbavaka.common.exception.specifics;

import org.springframework.http.HttpStatus;
import org.vvs.valimbavaka.common.exception.BaseException;

/** 409 Conflict - doublon, contrainte violee. */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
