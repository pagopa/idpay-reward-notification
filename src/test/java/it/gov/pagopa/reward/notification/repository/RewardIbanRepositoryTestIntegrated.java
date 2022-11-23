package it.gov.pagopa.reward.notification.repository;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
public class RewardIbanRepositoryTestIntegrated extends RewardIbanRepositoryTest{
}