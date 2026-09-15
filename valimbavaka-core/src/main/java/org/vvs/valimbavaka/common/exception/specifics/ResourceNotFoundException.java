package org.vvs.valimbavaka.common.exception.specifics;

import org.springframework.http.HttpStatus;
import org.vvs.valimbavaka.common.exception.BaseException;

/** 404 Not Found. */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) { super(HttpStatus.NOT_FOUND, message); }
}