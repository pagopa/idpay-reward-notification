package it.gov.pagopa.reward.notification.service.imports;

import it.gov.pagopa.reward.notification.dto.controller.FeedbackImportFilter;
import it.gov.pagopa.reward.notification.dto.controller.RewardImportsDTO;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationImport2ImportsDTOMapper;
import it.gov.pagopa.reward.notification.dto.mapper.RewardOrganizationImportErrors2ErrorsCsvMapper;
import it.gov.pagopa.reward.notification.repository.RewardOrganizationImportsRepository;
import it.gov.pagopa.reward.notification.service.csv.out.writer.FeedbackImportErrorsCsvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OrganizationImportsServiceImpl implements OrganizationImportsService {

    private final RewardOrganizationImportsRepository importsRepository;
    private final RewardOrganizationImport2ImportsDTOMapper rewardOrganizationImport2ImportsDTOMapper;
    private final FeedbackImportErrorsCsvService feedbackImportErrorsCsvService;
    private final RewardOrganizationImportErrors2ErrorsCsvMapper errors2ErrorsCsvMapper;

    public OrganizationImportsServiceImpl(
            RewardOrganizationImportsRepository rewardOrganizationImportsRepository,
            RewardOrganizationImport2ImportsDTOMapper rewardOrganizationImport2ImportsDTOMapper,
            FeedbackImportErrorsCsvService feedbackImportErrorsCsvService,
            RewardOrganizationImportErrors2ErrorsCsvMapper errors2ErrorsCsvMapper) {
        this.importsRepository = rewardOrganizationImportsRepository;
        this.rewardOrganizationImport2ImportsDTOMapper = rewardOrganizationImport2ImportsDTOMapper;
        this.feedbackImportErrorsCsvService = feedbackImportErrorsCsvService;
        this.errors2ErrorsCsvMapper = errors2ErrorsCsvMapper;
    }

    @Override
    public Flux<RewardImportsDTO> findAllBy(String organizationId, String initiativeId, Pageable pageable, FeedbackImportFilter filters) {
        return importsRepository
                .findAllBy(organizationId, initiativeId, pageable, filters)
                .map(rewardOrganizationImport2ImportsDTOMapper);
    }

    @Override
    public Mono<Long> countAll(String organizationId, String initiativeId, FeedbackImportFilter filters) {
        return importsRepository.countAll(organizationId, initiativeId, filters);
    }

    @Override
    public Mono<Page<RewardImportsDTO>> findAllPaged(String organizationId, String initiativeId, Pageable pageable, FeedbackImportFilter filters) {

        return findAllBy(organizationId, initiativeId, pageable, filters)
                .collectList()
                .zipWith(countAll(organizationId, initiativeId, filters))
                .map(t -> new PageImpl<>(t.getT1(), pageable, t.getT2()));
    }

    @Override
    public Mono<String> getErrorsCsvByImportId(String organizationId, String initiativeId, String importId) {
        return importsRepository
                .findByImportId(organizationId, initiativeId, importId)
                .flatMapMany(r -> Flux.fromIterable(r.getErrors()))
                .map(errors2ErrorsCsvMapper::apply)
                .collectList()
                .map(feedbackImportErrorsCsvService::writeCsv);
    }
}
