package com.group2.library_management.dto.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.group2.library_management.entity.BorrowingReceipt;
import com.group2.library_management.entity.enums.BorrowingStatus;

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
        List<BorrowingDetailResponse> borrowingDetails,
        List<BorrowingRequestDetailResponse> borrowingRequestDetails) {
    public static BorrowingReceiptResponse fromEntity(BorrowingReceipt entity,
            boolean includeDetails) {
        List<BorrowingDetailResponse> details = Collections.emptyList();
        List<BorrowingRequestDetailResponse> requestDetails = Collections.emptyList();

        if (includeDetails) {
            if (entity.getBorrowingDetails() != null && !entity.getBorrowingDetails().isEmpty()) {
                details = entity.getBorrowingDetails().stream()
                        .map(BorrowingDetailResponse::fromEntity)
                        .collect(Collectors.toList());
            }
            
            if (entity.getBorrowingRequestDetails() != null && !entity.getBorrowingRequestDetails().isEmpty()) {
                requestDetails = entity.getBorrowingRequestDetails().stream()
                        .map(BorrowingRequestDetailResponse::fromEntity)
                        .collect(Collectors.toList());
            }
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
                details,
                requestDetails);
    }

    public static BorrowingReceiptResponse fromEntity(BorrowingReceipt entity) {
        return fromEntity(entity, false);
    }
}
