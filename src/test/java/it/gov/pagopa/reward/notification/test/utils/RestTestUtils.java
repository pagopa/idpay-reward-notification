package it.gov.pagopa.reward.notification.test.utils;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class RestTestUtils {
    public static WireMockConfiguration getWireMockConfiguration(String stubPath){
        return wireMockConfig()
                .dynamicPort()
                .usingFilesUnderClasspath("src/test/resources"+stubPath);
    }
}
