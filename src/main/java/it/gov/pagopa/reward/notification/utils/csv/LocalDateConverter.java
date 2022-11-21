package it.gov.pagopa.reward.notification.utils.csv;

import com.opencsv.bean.AbstractBeanField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter extends AbstractBeanField<String, LocalDate> {
    @Override
    protected LocalDate convert(String s) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(s, formatter);
    }
}