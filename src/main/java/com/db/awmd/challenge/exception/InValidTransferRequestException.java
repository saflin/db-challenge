package com.db.awmd.challenge.exception;

/**
 * Exception for Invalid Fund Transfer Request.
 */
public class InValidTransferRequestException extends RuntimeException {
    public InValidTransferRequestException(String message){
        super(message);
    }
}
