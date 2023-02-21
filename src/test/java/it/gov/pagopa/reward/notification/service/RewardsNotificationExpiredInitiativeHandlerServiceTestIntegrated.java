package it.gov.pagopa.reward.notification.service;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable
public class RewardsNotificationExpiredInitiativeHandlerServiceTestIntegrated extends RewardsNotificationExpiredInitiativeHandlerServiceIntegrationTest{
}
