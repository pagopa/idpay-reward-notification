package it.gov.pagopa.reward.notification.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

class UtilsTest {
    @Test
    void getHeaderValueTest(){
        Message<String> msg = MessageBuilder
                .withPayload("")
                .setHeader("HEADERNAME", "HEADERVALUE".getBytes(StandardCharsets.UTF_8))
                .build();
        Assertions.assertNull(Utils.getHeaderValue(msg, "NOTEXISTS"));
        Assertions.assertEquals("HEADERVALUE", Utils.getHeaderValue(msg, "HEADERNAME"));
    }
}
