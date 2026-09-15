package org.vvs.valimbavaka.common.exception.specifics;

import org.springframework.http.HttpStatus;
import org.vvs.valimbavaka.common.exception.BaseException;

/** 400 Bad Request — erreur de validation métier. */
public class DomainException extends BaseException {

    public DomainException(String message) { super(HttpStatus.BAD_REQUEST, message); }
    public DomainException(String message, Throwable cause) { super(HttpStatus.BAD_REQUEST, message, cause); }
}