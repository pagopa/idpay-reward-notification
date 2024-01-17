package it.gov.pagopa.reward.notification.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

public class WalletInvocationException extends ServiceException {

    public WalletInvocationException(String code, String message) {
        this(code, message,null, false, null);
    }

    public WalletInvocationException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }
}
