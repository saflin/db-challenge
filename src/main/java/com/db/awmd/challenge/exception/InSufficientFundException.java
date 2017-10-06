package com.db.awmd.challenge.exception;

/**
 * Exception class to represent InSufficient Balance in the account.
 */
public class InSufficientFundException extends RuntimeException {
    public InSufficientFundException(final String message) {
        super(message);
    }
}
