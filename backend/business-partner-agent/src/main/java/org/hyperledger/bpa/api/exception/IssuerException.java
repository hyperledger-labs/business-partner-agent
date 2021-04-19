package org.hyperledger.bpa.api.exception;

public class IssuerException extends RuntimeException {

    private static final long serialVersionUID = -5913572693089903914L;

    public IssuerException(String message) {
        super(message);
    }

    public IssuerException(String message, Throwable t) {
        super(message, t);
    }
}
