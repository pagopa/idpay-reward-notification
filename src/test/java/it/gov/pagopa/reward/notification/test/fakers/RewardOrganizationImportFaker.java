package it.gov.pagopa.reward.notification.test.fakers;

import it.gov.pagopa.reward.notification.dto.mapper.StorageEvent2OrganizationImportMapper;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;

public class RewardOrganizationImportFaker {
    private static final StorageEvent2OrganizationImportMapper mapper = new StorageEvent2OrganizationImportMapper();

    public static RewardOrganizationImport mockInstance(Integer bias) {
        return mapper.apply(StorageEventDtoFaker.mockInstance(bias));
    }
}
