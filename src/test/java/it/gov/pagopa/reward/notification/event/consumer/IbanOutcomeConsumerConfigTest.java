package it.gov.pagopa.reward.notification.event.consumer;

import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.repository.RewardIbanRepository;
import it.gov.pagopa.reward.notification.test.fakers.IbanRequestDTOFaker;
import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.IntStream;

class IbanOutcomeConsumerConfigTest extends BaseIntegrationTest {

    @Autowired
    private RewardIbanRepository rewardIbanRepository;

    @Test
    void ibanOutcomeConsumer() {
        int ibansIntoDB = 100;
        //TODO
        inizializingDB(ibansIntoDB);
        //delete, publish into outcomeTopic

    }

    private void inizializingDB(int ibansIntoDB) {
        List<String> messagesIbanRequest = IntStream.range(0, ibansIntoDB)
                .mapToObj(IbanRequestDTOFaker::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList();

        messagesIbanRequest.forEach(m -> publishIntoEmbeddedKafka(topicIbanRequest,null, null, m));

        IbanRequestConsumerConfigTest.waitForIbanStored(ibansIntoDB,rewardIbanRepository);
    }
}