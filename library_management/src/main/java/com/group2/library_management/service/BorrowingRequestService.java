package com.group2.library_management.service;

import com.group2.library_management.dto.request.BorrowingRequest;
import com.group2.library_management.dto.response.BorrowingRequestResponse;

public interface BorrowingRequestService {
    BorrowingRequestResponse createBorrowingRequestFromCart(BorrowingRequest request);
}
