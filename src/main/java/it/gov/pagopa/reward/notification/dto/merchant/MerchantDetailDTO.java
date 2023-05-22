package it.gov.pagopa.reward.notification.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantDetailDTO {
    
    private String initiativeId;
    private String businessName; //ragione sociale
    private String legalOfficeAddress;
    private String legalOfficeMunicipality; // comune sede legale
    private String legalOfficeProvince;
    private String legalOfficeZipCode;
    private String certifiedEmail;
    private String fiscalCode;
    private String vatNumber;
    private String status;
    private String iban;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime creationDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updateDate;
}
