package it.gov.pagopa.reward.notification.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private String fiscalCode;
    private String name; // TODO to fill
    private String surname; // TODO to fill
}
