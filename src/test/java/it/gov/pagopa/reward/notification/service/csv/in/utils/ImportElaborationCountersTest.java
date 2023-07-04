package it.gov.pagopa.reward.notification.service.csv.in.utils;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import it.gov.pagopa.reward.notification.model.RewardOrganizationImport;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class ImportElaborationCountersTest {

    @Test
    void updateWithException() {
        List<CsvException> e = provideExceptionList();

        ImportElaborationCounters counters = new ImportElaborationCounters();

        ImportElaborationCounters.updateWithException(counters, e);

        Assertions.assertEquals(4, counters.getRewardsResulted());
        Assertions.assertEquals(4, counters.getRewardsResultedError());

        Assertions.assertEquals(1, counters.getRewardsResultedOk());
        Assertions.assertEquals(1, counters.getRewardsResultedOkError());

        List<RewardOrganizationImport.RewardOrganizationImportError> expectedErrors = List.of(
                new RewardOrganizationImport.RewardOrganizationImportError(-2, RewardFeedbackConstants.ImportFeedbackRowErrors.GENERIC_ERROR),
                new RewardOrganizationImport.RewardOrganizationImportError(1, RewardFeedbackConstants.ImportFeedbackRowErrors.GENERIC_ERROR),
                new RewardOrganizationImport.RewardOrganizationImportError(2, RewardFeedbackConstants.ImportFeedbackRowErrors.INVALID_DATE),
                new RewardOrganizationImport.RewardOrganizationImportError(3, RewardFeedbackConstants.ImportFeedbackRowErrors.INVALID_DATE)
        );
        Assertions.assertEquals(expectedErrors, counters.getErrors());
    }

    private static List<CsvException> provideExceptionList() {
        CsvDataTypeMismatchException csvDateExceptionNoLine = new CsvDataTypeMismatchException();
        csvDateExceptionNoLine.setLineNumber(2);

        String[] koLine = new String[]{"uniqueId","KO","","cro","2023-07-04"};
        CsvDataTypeMismatchException csvDateExceptionWithKoLine = new CsvDataTypeMismatchException(null, LocalDate.class);
        csvDateExceptionWithKoLine.setLine(koLine);
        csvDateExceptionWithKoLine.setLineNumber(3);

        String[] okLine = new String[]{"uniqueId","OK - ORDINE ESEGUITO","","cro","2023-07-04"};
        CsvDataTypeMismatchException csvDateExceptionWithOkLine = new CsvDataTypeMismatchException(null, LocalDate.class);
        csvDateExceptionWithOkLine.setLine(okLine);
        csvDateExceptionWithOkLine.setLineNumber(4);

        return List.of(new CsvException(), csvDateExceptionNoLine, csvDateExceptionWithKoLine, csvDateExceptionWithOkLine);
    }
}