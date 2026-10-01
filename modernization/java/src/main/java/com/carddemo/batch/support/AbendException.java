package com.carddemo.batch.support;

/** Unrecoverable batch error, the equivalent of the COBOL programs' 9999-ABEND-PROGRAM. */
public class AbendException extends RuntimeException {

    public AbendException(String message) {
        super(message);
    }
}
