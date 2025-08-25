package com.group2.library_management.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.group2.library_management.dto.response.BorrowingRequestResponse;
import com.group2.library_management.dto.response.BorrowingRequestDetailResponse;
import com.group2.library_management.entity.BorrowingReceipt;
import com.group2.library_management.entity.BorrowingRequestDetail;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BorrowingMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "borrowedDate", target = "proposedBorrowedDate")
    @Mapping(source = "dueDate", target = "proposedDueDate")
    @Mapping(source = "borrowingRequestDetails", target = "requestedItems")
    BorrowingRequestResponse toBorrowingRequestResponse(BorrowingReceipt receipt);

    @Mapping(source = "edition.id", target = "editionId")
    BorrowingRequestDetailResponse toBorrowingRequestDetailResponse(BorrowingRequestDetail detail);
}
