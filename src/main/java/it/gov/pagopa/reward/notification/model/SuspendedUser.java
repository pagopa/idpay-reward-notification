package it.gov.pagopa.reward.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "suspended_users")
@FieldNameConstants
public class SuspendedUser {
    @Id
    private String id;
    private String userId;
    private String initiativeId;
    private String organizationId;
    private LocalDateTime suspensionDate;

    public SuspendedUser(String userId, String initiativeId, String organizationId) {
        this.id = buildId(userId, initiativeId);
        this.userId = userId;
        this.initiativeId = initiativeId;
        this.organizationId = organizationId;
        this.suspensionDate = LocalDateTime.now();
    }

    public static String buildId(String userId, String initiativeId) {
        return userId + "_" + initiativeId;
    }
}
