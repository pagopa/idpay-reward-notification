package it.gov.pagopa.reward.notification.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.notification.dto.trx.Reward;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.enums.OperationType;
import it.gov.pagopa.common.utils.TestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class RewardTransactionDTOFaker {

    public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");

    private RewardTransactionDTOFaker() {
    }

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    private static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    private static int getRandomPositiveNumber(Integer bias, int bound) {
        return Math.abs(getRandom(bias).nextInt(bound));
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /**
     * @see #mockInstance(Integer) using INITIATIVEID
     */
    public static RewardTransactionDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    /**
     * It will return an example of {@link RewardTransactionDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardTransactionDTO mockInstance(Integer bias, String initiativeId) {
        return mockInstanceBuilder(bias, initiativeId).build();
    }

    public static RewardTransactionDTO.RewardTransactionDTOBuilder<?, ?> mockInstanceBuilder(Integer bias) {
        return mockInstanceBuilder(bias, "INITIATIVEID");
    }
    public static RewardTransactionDTO.RewardTransactionDTOBuilder<?, ?> mockInstanceBuilder(Integer bias, String initiativeId) {
        LocalDate trxDate = LocalDate.of(2022, getRandomPositiveNumber(bias, 11) + 1, getRandomPositiveNumber(bias, 27)+1);
        LocalTime trxTime = LocalTime.of(getRandomPositiveNumber(bias, 23), getRandomPositiveNumber(bias, 59), getRandomPositiveNumber(bias, 59));
        LocalDateTime trxDateTime = LocalDateTime.of(trxDate, trxTime);
        OffsetDateTime trxOffsetDate = OffsetDateTime.of(
                trxDateTime,
                ZONEID.getRules().getOffset(trxDateTime)
        );

        BigDecimal amount = TestUtils.bigDecimalValue(getRandomPositiveNumber(bias, 200));

        RewardTransactionDTO.RewardTransactionDTOBuilder<?, ?> out = RewardTransactionDTO.builder()
                .idTrxAcquirer("IDTRXACQUIRER%s".formatted(bias))
                .acquirerCode("ACQUIRERCODE%s".formatted(bias))
                .trxDate(trxOffsetDate)
                .hpan("HPAN%s".formatted(bias))
                .operationType("00")
                .circuitType("CIRCUITTYPE%s".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%s".formatted(bias))
                .correlationId("CORRELATIONID%s".formatted(bias))
                .amount(amount)
                .amountCurrency("AMOUNTCURRENCY%s".formatted(bias))
                .mcc("MCC%s".formatted(bias))
                .acquirerId("ACQUIRERID%s".formatted(bias))
                .merchantId("MERCHANTID%s".formatted(bias))
                .terminalId("TERMINALID%s".formatted(bias))
                .bin("BIN%s".formatted(bias))
                .senderCode("SENDERCODE%s".formatted(bias))
                .fiscalCode("FISCALCODE%s".formatted(bias))
                .vat("VAT%s".formatted(bias))
                .posType("POSTYPE%s".formatted(bias))
                .par("PAR%s".formatted(bias))
                .userId("USERID%s".formatted(bias))

                .effectiveAmount(amount)
                .trxChargeDate(trxOffsetDate)
                .operationTypeTranscoded(OperationType.CHARGE)

                .status("REWARDED")
                .rewards(Map.of(
                        initiativeId, new Reward(amount.multiply(BigDecimal.valueOf(0.1)).setScale(2, RoundingMode.HALF_DOWN))
                ));

        out.id(computeTrxId(out.build()));

        return out;
    }

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    public static String computeTrxId(RewardTransactionDTO trx) {
        return trx.getIdTrxAcquirer()
                .concat(trx.getAcquirerCode())
                .concat(trx.getTrxDate().atZoneSameInstant(ZONEID).toLocalDateTime().format(DATETIME_FORMATTER))
                .concat(trx.getOperationType())
                .concat(trx.getAcquirerId());
    }
}
