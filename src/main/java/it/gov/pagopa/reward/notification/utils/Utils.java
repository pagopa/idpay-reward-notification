package it.gov.pagopa.reward.notification.utils;

import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.notification.model.RewardsNotification;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public final class Utils {
    public static final DecimalFormat percentageFormatter = new DecimalFormat("0");

    private Utils() {}

    public static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String PAYLOAD_FIELD_USER_ID = "\"%s\"".formatted(TransactionDTO.Fields.userId);

    /**
     * It will read userId field from {@link RewardTransactionDTO} payload
     */
    public static String readUserId(String payload) {
        int userIdIndex = payload.indexOf(PAYLOAD_FIELD_USER_ID);
        if (userIdIndex > -1) {
            String afterUserId = payload.substring(userIdIndex + 8);
            final int afterOpeningQuote = afterUserId.indexOf('"') + 1;
            return afterUserId.substring(afterOpeningQuote, afterUserId.indexOf('"', afterOpeningQuote));
        }
        return null;
    }

    public static String filePath2FileName(String filePath) {
        return Path.of(filePath).getFileName().toString();
    }

    public static RefundType getRefundType(RewardsNotification notification) {
        if (notification.getOrdinaryId() != null) {
            return RefundType.REMEDIAL;
        } else {
            return RefundType.ORDINARY;
        }
    }

    /** It will return the percentage of value compared to total, multiplied by 100 in order to return an integer representing the percentage having scale 2 */
    public static long calcPercentage(long value, long total) {
        return (long) ((((double) value) / total) * 100_00);
    }

    /** It will accept a long representing a percentage (100 -> 100%)  and will turn into a formatted string */
    public static String formatAsPercentage(Long p) {
        return percentageFormatter.format((double) p / 100);
    }

    public enum RefundType {
        ORDINARY,
        REMEDIAL
    }
}
