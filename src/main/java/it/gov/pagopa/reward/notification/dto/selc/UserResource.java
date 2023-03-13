package it.gov.pagopa.reward.notification.dto.selc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResource {

    private UUID id;
    private String name;
    private String surname;
    private String email;
    private List<String> roles;

}
