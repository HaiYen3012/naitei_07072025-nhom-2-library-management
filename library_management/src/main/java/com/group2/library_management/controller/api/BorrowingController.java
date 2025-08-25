package com.group2.library_management.controller.api;

import com.group2.library_management.common.constants.Endpoints;
import com.group2.library_management.dto.request.BorrowingRequest;
import com.group2.library_management.dto.response.BaseApiResponse;
import com.group2.library_management.dto.response.BorrowingRequestResponse;
import com.group2.library_management.service.BorrowingRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("apiBorrowingController")
@RequestMapping(Endpoints.ApiV1.Borrowings.BASE_URL)
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingRequestService borrowingRequestService;
    private final MessageSource messageSource;

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @PostMapping(Endpoints.ApiV1.Borrowings.CREATE_REQUEST_ACTION)
    public ResponseEntity<BaseApiResponse<BorrowingRequestResponse>> createBorrowingRequest(@Valid @RequestBody BorrowingRequest request) {
        BorrowingRequestResponse responseData = borrowingRequestService.createBorrowingRequestFromCart(request);
        
        String successMessage = getMessage("success.borrowing.request_created");
        BaseApiResponse<BorrowingRequestResponse> response = new BaseApiResponse<>(
                HttpStatus.CREATED.value(),
                responseData,
                successMessage
        );
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
