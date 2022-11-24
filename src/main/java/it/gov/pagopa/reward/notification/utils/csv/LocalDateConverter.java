package it.gov.pagopa.reward.notification.utils.csv;

import com.opencsv.bean.AbstractBeanField;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter extends AbstractBeanField<String, LocalDate> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected LocalDate convert(String s) {
        if(StringUtils.isEmpty(s)){
            return null;
        } else {
            return LocalDate.parse(s, formatter);
        }
    }
}