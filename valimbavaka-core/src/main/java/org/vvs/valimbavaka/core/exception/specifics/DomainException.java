package org.vvs.valimbavaka.core.exception.specifics;

import org.vvs.valimbavaka.core.exception.BaseException;

/** 400 Bad Request — erreur de validation métier. */
public class DomainException extends BaseException {
    public DomainException(String message) { super(message); }
    public DomainException(String message, Throwable cause) { super(message, cause); }
}