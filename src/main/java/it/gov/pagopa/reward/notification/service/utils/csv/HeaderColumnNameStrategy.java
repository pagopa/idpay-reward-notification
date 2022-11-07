package it.gov.pagopa.reward.notification.service.utils.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/** A strategy to build a csv having column ordered as declaration order and columnName as defined using {@link CsvBindByName} annotation*/
@Slf4j
public class HeaderColumnNameStrategy<T> extends HeaderColumnNameMappingStrategy<T> {

    public HeaderColumnNameStrategy(Class<? extends T> clazz) {
        setType(clazz);

        // Build the header line which respects the declaration order
        String headerLine = Arrays.stream(clazz.getDeclaredFields())
                .map(field -> field.getAnnotation(CsvBindByName.class))
                .filter(Objects::nonNull)
                .map(CsvBindByName::column)
                .collect(Collectors.joining(","));

        // Initialize strategy by reading a CSV with header only
        try (StringReader reader = new StringReader(headerLine)) {
            CsvToBean<T> sampleCsv = new CsvToBeanBuilder<T>(reader)
                    .withType(clazz)
                    .withMappingStrategy(this)
                    .build();
            sampleCsv.forEach(l -> log.trace("Loading header position: {}", l));
        }
    }
}
