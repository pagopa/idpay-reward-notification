package it.gov.pagopa.reward.notification.dto.controller.detail;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportPageDTO {
    @JsonProperty(value = "content")
    private List<ExportDetailDTO> content;
    @JsonProperty(value = "pageNo")
    private int pageNo;
    @JsonProperty(value = "pageSize")
    private int pageSize;
    @JsonProperty(value = "totalElements")
    private long totalElements;
    @JsonProperty(value = "totalPages")
    private int totalPages;
}
