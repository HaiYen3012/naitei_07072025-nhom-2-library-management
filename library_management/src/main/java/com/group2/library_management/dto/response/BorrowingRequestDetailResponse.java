package com.group2.library_management.dto.response;

import com.group2.library_management.entity.BorrowingRequestDetail;
import lombok.Builder;

@Builder
public record BorrowingRequestDetailResponse(
        Integer id,
        Integer editionId,
        String title,
        String isbn,
        int quantity
) {
    public static BorrowingRequestDetailResponse fromEntity(BorrowingRequestDetail entity) {
        if (entity == null || entity.getEdition() == null) {
            return BorrowingRequestDetailResponse.builder().build();
        }
        return BorrowingRequestDetailResponse.builder()
                .id(entity.getId())
                .editionId(entity.getEdition().getId())
                .title(entity.getEdition().getTitle())
                .isbn(entity.getEdition().getIsbn())
                .quantity(entity.getQuantity())
                .build();
    }
}
