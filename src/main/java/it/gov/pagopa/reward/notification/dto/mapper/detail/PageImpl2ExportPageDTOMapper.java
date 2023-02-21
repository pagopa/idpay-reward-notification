package it.gov.pagopa.reward.notification.dto.mapper.detail;

import it.gov.pagopa.reward.notification.dto.controller.detail.ExportDetailDTO;
import it.gov.pagopa.reward.notification.dto.controller.detail.ExportPageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PageImpl2ExportPageDTOMapper {

    public ExportPageDTO apply(PageImpl<ExportDetailDTO> page) {
        return ExportPageDTO.builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public ExportPageDTO apply(Pageable pageable) {
        Page<ExportDetailDTO> page = Page.empty(pageable);

        return ExportPageDTO.builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
