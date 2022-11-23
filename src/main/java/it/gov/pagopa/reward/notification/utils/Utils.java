package it.gov.pagopa.reward.notification.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.notification.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.notification.dto.trx.TransactionDTO;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class Utils {
    private Utils(){}

    public static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    /** It will try to deserialize a message, eventually notifying the error  */
    public static <T> T deserializeMessage(Message<String> message, ObjectReader objectReader, Consumer<Throwable> onError) {
        try {
            return objectReader.readValue(message.getPayload());
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    private static final String PAYLOAD_FIELD_USER_ID = "\"%s\"".formatted(TransactionDTO.Fields.userId);
    /** It will read userId field from {@link RewardTransactionDTO} payload */
    public static String readUserId(String payload) {
        int userIdIndex = payload.indexOf(PAYLOAD_FIELD_USER_ID);
        if(userIdIndex>-1){
            String afterUserId = payload.substring(userIdIndex+8);
            final int afterOpeningQuote = afterUserId.indexOf('"') + 1;
            return afterUserId.substring(afterOpeningQuote, afterUserId.indexOf('"', afterOpeningQuote));
        }
        return null;
    }

    public static Long euro2Cents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }
}
