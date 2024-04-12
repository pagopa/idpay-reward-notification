package it.gov.pagopa.reward.notification.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class WalletInvocationException extends ServiceException {

    public WalletInvocationException(String code, String message) {
        this(code, message,false, null);
    }

    public WalletInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }
}
