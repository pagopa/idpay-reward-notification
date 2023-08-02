package it.gov.pagopa.reward.notification.event.consumer;

import com.mongodb.MongoException;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.notification.BaseIntegrationTest;
import it.gov.pagopa.reward.notification.dto.commands.CommandOperationDTO;
import it.gov.pagopa.reward.notification.model.*;
import it.gov.pagopa.reward.notification.repository.*;
import it.gov.pagopa.reward.notification.test.fakers.RewardNotificationRuleFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationExportsFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardOrganizationImportFaker;
import it.gov.pagopa.reward.notification.test.fakers.RewardsNotificationFaker;
import it.gov.pagopa.reward.notification.utils.CommandsConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.reward.notification.service.commands.CommandsMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.notification.service.commands.ops.DeleteInitiativeServiceImpl=WARN",
})
class CommandsConsumerConfigTest extends BaseIntegrationTest {
    private final String INITIATIVEID = "INITIATIVEID_%d";
    private final String ORGANIZATIONID = "ORGANIZATIONID%d";
    private final String FILEPATH = "directory/fileName%d";
    private final Set<String> INITIATIVES_DELETED = new HashSet<>();
    @SpyBean
    private RewardNotificationRuleRepository rewardNotificationRuleRepository;
    @Autowired
    private RewardOrganizationExportsRepository rewardOrganizationExportsRepository;

    @Autowired
    private RewardOrganizationImportsRepository rewardOrganizationImportsRepository;

    @Autowired
    private RewardsNotificationRepository rewardsNotificationRepository;
    @Autowired
    private RewardIbanRepository rewardIbanRepository;
    @Autowired
    private RewardsRepository rewardsRepository;
    @Autowired
    private RewardsSuspendedUserRepository rewardsSuspendedUserRepository;

    @Test
    void test() {
        int validMessages = 100;
        int notValidMessages = errorUseCases.size();
        long maxWaitingMs = 30000;

        List<String> commandsPayloads = new ArrayList<>(notValidMessages+validMessages);
        commandsPayloads.addAll(IntStream.range(0,notValidMessages).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        commandsPayloads.addAll(buildValidPayloads(notValidMessages, notValidMessages+validMessages));

        long timeStart=System.currentTimeMillis();
        commandsPayloads.forEach(cp -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicCommands, null, null, cp));
        long timePublishingEnd = System.currentTimeMillis();

        waitForLastStorageChange(validMessages/2);

        long timeEnd=System.currentTimeMillis();

        checkRepositories();
        checkErrorsPublished(notValidMessages, maxWaitingMs, errorUseCases);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert db stored count (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                commandsPayloads.size(),
                validMessages,
                notValidMessages,
                timePublishingEnd - timeStart,
                timeEnd - timePublishingEnd,
                timeEnd - timeStart
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = kafkaTestUtilitiesService.checkCommittedOffsets(topicCommands, groupIdCommands, commandsPayloads.size());
        long timeCommitCheckEnd = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitCheckEnd - timeCommitCheckStart,
                srcCommitOffsets
        );

    }


    private long waitForLastStorageChange(int n) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        TestUtils.waitFor(()->(countSaved[0]=rewardsSuspendedUserRepository.findAll().count().block()) == n, ()->"Expected %d saved users in db, read %d".formatted(n, countSaved[0]), 60, 1000);
        return countSaved[0];
    }

    private List<String> buildValidPayloads(int startValue, int messagesNumber) {
        return IntStream.range(startValue, messagesNumber)
                .mapToObj(i -> {
                    initializeDB(i);
                    CommandOperationDTO command = CommandOperationDTO.builder()
                            .entityId(INITIATIVEID.formatted(i))
                            .operationTime(LocalDateTime.now())
                            .build();

                    if(i%2 == 0){
                        INITIATIVES_DELETED.add(command.getEntityId());
                        command.setOperationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE);
                    } else {
                        command.setOperationType("ANOTHER_TYPE");
                    }
                    return command;
                })
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private void initializeDB(int bias) {
        RewardNotificationRule rewardNotificationRule = RewardNotificationRuleFaker.mockInstanceBuilder(bias)
                .initiativeId(INITIATIVEID.formatted(bias))
                .organizationId(ORGANIZATIONID.formatted(bias))
                .build();
        rewardNotificationRuleRepository.save(rewardNotificationRule).block();

        RewardOrganizationExport rewardOrganizationExport = RewardOrganizationExportsFaker.mockInstanceBuilder(bias)
                .initiativeId(INITIATIVEID.formatted(bias))
                .organizationId(ORGANIZATIONID.formatted(bias))
                .filePath(FILEPATH.formatted(bias))
                .build();
        rewardOrganizationExportsRepository.save(rewardOrganizationExport).block();

        RewardOrganizationImport rewardOrganizationImport = RewardOrganizationImportFaker.mockInstance(bias);
        rewardOrganizationImport.setInitiativeId(INITIATIVEID.formatted(bias));
        rewardOrganizationImport.setOrganizationId(ORGANIZATIONID.formatted(bias));
        rewardOrganizationImport.setFilePath(FILEPATH.formatted(bias));
        rewardOrganizationImportsRepository.save(rewardOrganizationImport).block();

        RewardsNotification rewardsNotification = RewardsNotificationFaker.mockInstanceBuilder(bias, INITIATIVEID.formatted(bias), LocalDate.now()).build();
        rewardsNotificationRepository.save(rewardsNotification).block();

        RewardIban rewardIban = RewardIban.builder()
                .userId("USERID")
                .initiativeId(INITIATIVEID.formatted(bias))
                .build();
        rewardIbanRepository.save(rewardIban).block();

        Rewards rewards = Rewards.builder()
                .id("ID%d".formatted(bias))
                .userId("USERID")
                .initiativeId(INITIATIVEID.formatted(bias))
                .build();
        rewardsRepository.save(rewards).block();

        RewardSuspendedUser rewardSuspendedUser = RewardSuspendedUser.builder()
                .id("ID%d".formatted(bias))
                .userId("USERID")
                .initiativeId(INITIATIVEID.formatted(bias))
                .build();
        rewardsSuspendedUserRepository.save(rewardSuspendedUser).block();

    }

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"entityId\":\"ENTITYID_ERROR([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        String useCaseJsonNotExpected = "{\"entityId\":\"ENTITYID_ERROR0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_COMMANDS] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"entityId\":\"ENTITYID_ERROR1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_COMMANDS] Unexpected JSON", jsonNotValid)
        ));

        final String errorInitiativeId = "ENTITYID_ERROR2";
        CommandOperationDTO commandOperationError = CommandOperationDTO.builder()
                .entityId(errorInitiativeId)
                .operationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE)
                .operationTime(LocalDateTime.now())
                .build();
        String commandOperationErrorString = TestUtils.jsonSerializer(commandOperationError);
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new MongoException("Command error dummy"))
                            .when(rewardNotificationRuleRepository).deleteById(errorInitiativeId);
                    return commandOperationErrorString;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_NOTIFICATION_COMMANDS] An error occurred evaluating commands", commandOperationErrorString)
        ));
    }

    private void checkRepositories() {
        Assertions.assertTrue(rewardNotificationRuleRepository.findAll().toStream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
        Assertions.assertTrue(rewardOrganizationExportsRepository.findAll().toStream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
        Assertions.assertTrue(rewardOrganizationImportsRepository.findAll().toStream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
        Assertions.assertTrue(rewardsNotificationRepository.findAll().toStream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
        Assertions.assertTrue(rewardIbanRepository.findAll().toStream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
        Assertions.assertTrue(rewardsRepository.findAll().toStream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
        Assertions.assertTrue(rewardsSuspendedUserRepository.findAll().toStream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicCommands, groupIdCommands, errorMessage, errorDescription, expectedPayload, null);
    }
}