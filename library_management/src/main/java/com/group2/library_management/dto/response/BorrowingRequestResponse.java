package com.group2.library_management.dto.response;

import com.group2.library_management.entity.enums.BorrowingStatus;
import java.time.LocalDateTime;
import java.util.List;

public record BorrowingRequestResponse(
    Integer id,
    Integer userId,
    LocalDateTime proposedBorrowedDate, 
    LocalDateTime proposedDueDate, 
    BorrowingStatus status,
    List<BorrowingRequestDetailResponse> requestedItems,
    LocalDateTime createdAt
) {}
