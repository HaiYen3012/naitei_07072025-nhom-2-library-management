package com.group2.library_management.dto.response;

import com.group2.library_management.entity.BorrowingReceipt;
import com.group2.library_management.entity.enums.BorrowingStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record BorrowingReceiptResponse(
        Integer id,
        String borrowerName,
        String borrowerEmail,
        Integer borrowerId,
        LocalDateTime requestDate,
        LocalDateTime borrowedDate,
        LocalDateTime dueDate,
        LocalDateTime updatedAt,
        BorrowingStatus status,
        String rejectedReason,
        List<BorrowingDetailResponse> borrowingDetails
) {
    public static BorrowingReceiptResponse fromEntity(BorrowingReceipt entity,
                                                      boolean includeDetails) {
        List<BorrowingDetailResponse> details = Collections.emptyList();

        if (includeDetails && entity.getBorrowingDetails() instanceof List<?> list && !list.isEmpty()) {
            details = entity.getBorrowingDetails().stream()
                    .map(BorrowingDetailResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        return new BorrowingReceiptResponse(
                entity.getId(),
                entity.getUser().getName(),
                entity.getUser().getEmail(),
                entity.getUser().getId(),
                entity.getCreatedAt(),
                entity.getBorrowedDate(),
                entity.getDueDate(),
                entity.getUpdatedAt(),
                entity.getStatus(),
                entity.getRejectedReason(),
                details
        );
    }

    public static BorrowingReceiptResponse fromEntity(BorrowingReceipt entity) {
        return fromEntity(entity, false);
    }
}
