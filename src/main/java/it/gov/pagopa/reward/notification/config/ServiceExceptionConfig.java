package it.gov.pagopa.reward.notification.config;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.notification.exception.custom.WalletInvocationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ServiceExceptionConfig {

    @Bean
    public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper() {
        Map<Class<? extends ServiceException>, HttpStatus> exceptionMap = new HashMap<>();

        // InternalServerError
        exceptionMap.put(WalletInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);

        return exceptionMap;
    }
}
