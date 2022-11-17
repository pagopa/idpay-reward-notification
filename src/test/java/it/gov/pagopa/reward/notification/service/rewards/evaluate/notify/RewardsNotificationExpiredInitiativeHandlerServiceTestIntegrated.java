package it.gov.pagopa.reward.notification.service.rewards.evaluate.notify;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
public class RewardsNotificationExpiredInitiativeHandlerServiceTestIntegrated extends RewardsNotificationExpiredInitiativeHandlerServiceIntegrationTest{
}
