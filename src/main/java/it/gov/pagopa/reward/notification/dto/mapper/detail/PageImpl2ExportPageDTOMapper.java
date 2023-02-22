package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.RewardNotificationDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportContentPageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PageImpl2ExportPageDTOMapper {

    public ExportContentPageDTO apply(PageImpl<RewardNotificationDTO> page) {
        return ExportContentPageDTO.builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public ExportContentPageDTO apply(Pageable pageable) {
        Page<RewardNotificationDTO> page = Page.empty(pageable);

        return ExportContentPageDTO.builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
