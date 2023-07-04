package it.gov.pagopa.common.utils.csv;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter extends AbstractBeanField<String, LocalDate> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected LocalDate convert(String s) throws CsvDataTypeMismatchException {
        if(StringUtils.isEmpty(s)){
            return null;
        } else {
            try{
                return LocalDate.parse(s, formatter);
            } catch (DateTimeException e){
                throw new CsvDataTypeMismatchException(null, LocalDate.class, e.getMessage());
            }
        }
    }
}