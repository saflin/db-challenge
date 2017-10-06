package com.db.awmd.challenge.exception;

/**
 * Exception class to represent account not found in the system.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(final String message){
        super(message);
    }
}
