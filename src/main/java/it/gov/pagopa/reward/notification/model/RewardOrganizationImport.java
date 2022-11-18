package it.gov.pagopa.reward.notification.model;

import it.gov.pagopa.reward.notification.enums.RewardOrganizationImportStatus;
import it.gov.pagopa.reward.notification.utils.RewardFeedbackConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Document(collection = "rewards_organization_imports")
@FieldNameConstants
public class RewardOrganizationImport {

    @Id
    private String filePath;
    private String initiativeId;
    private String organizationId;
    private LocalDateTime feedbackDate;
    private String eTag;
    private Integer contentLength;
    private String url;
    @Builder.Default private Long rewardsResulted=0L;
    @Builder.Default private Long rewardsResultedError=0L;
    @Builder.Default private Long rewardsResultedOk=0L;
    @Builder.Default private Long rewardsResultedOkError=0L;
    @Builder.Default private Long percentageResulted=0L;
    @Builder.Default private Long percentageResultedOk=0L;
    @Builder.Default private Long percentageResultedOkElab=0L;
    private LocalDateTime elabDate;
    @Builder.Default private List<String> exportIds=new ArrayList<>();
    private RewardOrganizationImportStatus status;
    @Builder.Default private Integer errorsSize=0;
    @Builder.Default private List<RewardOrganizationImportError> errors = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RewardOrganizationImportError {
        private Integer row;
        private String errorCode;
        private String errorDescription;

        public RewardOrganizationImportError(RewardFeedbackConstants.ImportFileErrors noRows) {
            this.row=-1;
            this.errorCode=noRows.name();
            this.errorDescription=noRows.description;
        }
    }
}
