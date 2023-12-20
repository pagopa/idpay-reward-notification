package it.gov.pagopa.reward.notification.config;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.reward.notification.utils.Utils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RewardNotificationErrorManagerConfig {

  @Bean
  ErrorDTO defaultErrorDTO() {
    return new ErrorDTO(
        Utils.ExceptionCode.GENERIC_ERROR,
        "A generic error occurred"
    );
  }

  @Bean
  ErrorDTO tooManyRequestsErrorDTO() {
    return new ErrorDTO(Utils.ExceptionCode.TOO_MANY_REQUESTS, "Too Many Requests");
  }
}
