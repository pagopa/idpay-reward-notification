package it.gov.pagopa.reward.notification.test.utils;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class RestTestUtils {
    public static WireMockConfiguration getWireMockConfiguration(int port, String host, String stubPath){
        return wireMockConfig().port(port)
                .bindAddress(host)
                .usingFilesUnderClasspath("src/test/resources"+stubPath);
    }
}
