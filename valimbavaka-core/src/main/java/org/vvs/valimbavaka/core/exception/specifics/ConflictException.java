package org.vvs.valimbavaka.core.exception.specifics;

import org.vvs.valimbavaka.core.exception.BaseException;

/** 409 Conflict - doublon, contrainte violee. */
public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(message);
    }
}
