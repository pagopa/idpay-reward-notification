package it.gov.pagopa.reward.notification.service.iban;

import it.gov.pagopa.reward.notification.dto.iban.IbanOutcomeDTO;
import it.gov.pagopa.reward.notification.dto.mapper.IbanOutcomeDTO2RewardIbanMapper;
import it.gov.pagopa.reward.notification.model.RewardIban;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class RewardIbanServiceImpl implements RewardIbanService {

    private final RewardIbanRepository rewardIbanRepository;

    public RewardIbanServiceImpl(RewardIbanRepository rewardIbanRepository) {
        this.rewardIbanRepository = rewardIbanRepository;
    }

    @Override
    public Mono<RewardIban> save(RewardIban rewardIban) {
        return rewardIbanRepository.save(rewardIban);
    }

    @Override
    public Mono<RewardIban> deleteIban(IbanOutcomeDTO ibanOutcomeDTO) {
        return rewardIbanRepository.deleteByIdAndIban(IbanOutcomeDTO2RewardIbanMapper.buildId(ibanOutcomeDTO), ibanOutcomeDTO.getIban())
                .switchIfEmpty(Mono.defer(() -> {
                            log.info("UserId: {} initiativeId: {} and iban {} do not match into the DB", ibanOutcomeDTO.getUserId(), ibanOutcomeDTO.getInitiativeId(), ibanOutcomeDTO.getIban());
                            return Mono.empty();
                        }
                ));
    }
}