package it.gov.pagopa.reward.notification.service.rewards;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component given a rewarded trx, for each reward:
 * <ol>
 *     <li>duplicate check</li>
 *     <li>initiative retrieve</li>
 *     <li>reward notification upsert</li>
 * </ol>
 * */
public interface RewardsMediatorService {
    void execute(Flux<Message<String>> initiativeDTOFlux);
}
