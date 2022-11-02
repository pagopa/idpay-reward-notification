package it.gov.pagopa.reward.notification.controller;

import it.gov.pagopa.reward.notification.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.notification.model.RewardOrganizationExport;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationExportsRepository;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class NotificationControllerImpl implements NotificationController{

    private final RewardOrganizationExportsRepository rewardOrganizationExportsRepository;

    public NotificationControllerImpl(RewardOrganizationExportsRepository rewardOrganizationExportsRepository) {
        this.rewardOrganizationExportsRepository = rewardOrganizationExportsRepository;
    }

    @Override
    public Flux<RewardOrganizationExport> getExports() {
        return rewardOrganizationExportsRepository.findAll()
                .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND)));
    }
}
