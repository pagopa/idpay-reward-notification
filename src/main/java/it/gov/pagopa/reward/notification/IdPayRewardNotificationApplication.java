package it.gov.pagopa.reward.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class IdPayRewardNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayRewardNotificationApplication.class, args);
    }

}
