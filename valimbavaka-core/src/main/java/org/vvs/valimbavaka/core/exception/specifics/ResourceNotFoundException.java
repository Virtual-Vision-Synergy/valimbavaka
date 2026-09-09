package org.vvs.valimbavaka.core.exception.specifics;

import org.vvs.valimbavaka.core.exception.BaseException;

/** 404 Not Found. */
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) { super(message); }
}