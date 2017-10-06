package com.db.awmd.challenge.exception;

/**
 *  Represents fund transfer exception while processing transfer request.
 */
public class FundTransferException extends RuntimeException {

    public FundTransferException(final String message){
        super(message);
    }
}
