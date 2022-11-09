package it.gov.pagopa.reward.notification.test.utils.json;

import it.gov.pagopa.reward.notification.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

class PageModuleTest {

    @Test
    void test() {
        // Given
        String testString = "TEST";
        PageRequest pageRequest = PageRequest.of(0,1);
        PageImpl<String> page = new PageImpl<>(List.of(testString), pageRequest, 1);

        // When
        String serialized = TestUtils.jsonSerializer(page);
        // TODO deserialize

        // Then
        Assertions.assertNotNull(serialized);
        Assertions.assertEquals(
                "{\"content\":[\"%s\"],\"pageable\":{\"page\":0,\"size\":1,\"sort\":{\"orders\":[]}},\"total\":%d}"
                        .formatted(testString, 1),
                serialized
        );

        /*Assertions.assertNotNull(result);
        Assertions.assertEquals(page, result);*/

    }
}