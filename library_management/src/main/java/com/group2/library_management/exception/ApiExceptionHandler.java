package com.group2.library_management.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.group2.library_management.dto.response.BaseApiResponse;
import com.group2.library_management.dto.response.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice(basePackages = "com.group2.library_management.controller.api")
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final MessageSource messageSource;
    private static final int MAX_QUANTITY_PER_EDITION = 2;
    private static final int MAX_TOTAL_ITEMS_IN_CART = 5;

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    // xử lý lỗi không tìm thấy tài nguyên
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {  
        String errorMessage = getMessage("error.message.resource.not_found");
        String errorTitle = getMessage("error.title.resource.not_found");

        // tạo đối tượng ErrorResponse chi tiết
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        // đóng gói ErrorResponse vào BaseApiResponse
        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.NOT_FOUND.value(), // code
            errorResponse, // data
            errorTitle // message
        );

        // trả về ResponseEntity
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    // xử lý lỗi validation khi dữ liệu trong @RequestBody không hợp lệ
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        String errorMessage = getMessage("error.message.validation.failed");
        String errorTitle = getMessage("error.title.validation.failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            errorMessage,
            request.getRequestURI(),
            errors
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            errorResponse,
            errorTitle
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý lỗi validation cho các tham số trên phương thức (@RequestParam, @PathVariable).
     * Được kích hoạt khi class Controller đánh dấu @Validated.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            
            errors.put(fieldName, errorMessage);
        }

        String errorTitle = getMessage("error.title.validation.failed"); 
        String errorMessage = getMessage("error.message.validation.failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            errorMessage, 
            request.getRequestURI(),
            errors 
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            errorResponse,
            errorTitle
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.email.already_exists");
        String errorTitle = getMessage("error.title.data.conflict");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.CONFLICT.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.resource.not_found");
        String errorTitle = getMessage("error.title.resource.not_found");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.NOT_FOUND.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Xử lý lỗi khi người dùng nhập sai email hoặc mật khẩu.
     * Cả UsernameNotFoundException và BadCredentialsException đều trả về một thông báo chung
     * để tăng tính bảo mật, tránh để lộ thông tin email nào là hợp lệ.
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleBadCredentialsException(Exception ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.bad_credentials");
        String errorTitle = getMessage("error.title.auth.failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            errorMessage, 
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.UNAUTHORIZED.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Xử lý lỗi liên quan đến trạng thái tài khoản (bị khóa)
     */
    @ExceptionHandler({LockedException.class, DisabledException.class})
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleAccountStatusException(AccountStatusException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.account.locked");
        String errorTitle = getMessage("error.title.account.status");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(), 
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            errorMessage, 
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.FORBIDDEN.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.access.denied");
        String errorTitle = getMessage("error.title.access.denied");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            errorMessage, 
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.FORBIDDEN.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.user.not_found");
        String errorTitle = getMessage("error.title.resource.not_found");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            errorMessage, 
            request.getRequestURI()
        );
        
        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.NOT_FOUND.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleRefreshTokenExpiredException(RefreshTokenExpiredException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.refresh_token.expired");
        String errorTitle = getMessage("error.title.refresh_token.failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );
        
        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.FORBIDDEN.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleRefreshTokenNotFoundException(RefreshTokenNotFoundException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.refresh_token.not_found");
        String errorTitle = getMessage("error.title.refresh_token.failed"); 

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.FORBIDDEN.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AlreadyLoggedOutException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleAlreadyLoggedOutException(AlreadyLoggedOutException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.already_logged_out");
        String errorTitle = getMessage("error.title.logout.failed");
        
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(CartQuantityLimitException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleCartQuantityLimitException(CartQuantityLimitException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.cart.quantity_limit", MAX_QUANTITY_PER_EDITION);
        String errorTitle = getMessage("error.title.cart.operation_failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            errorMessage, 
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CartTotalItemLimitException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleCartTotalItemLimitException(CartTotalItemLimitException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.cart.total_item_limit", MAX_TOTAL_ITEMS_IN_CART);
        String errorTitle = getMessage("error.title.cart.operation_failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPrincipalTypeException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleInvalidPrincipalTypeException(InvalidPrincipalTypeException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.invalid_principal");
        String errorTitle = getMessage("error.title.auth.failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.UNAUTHORIZED.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidPickupDateException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleInvalidPickupDateException(InvalidPickupDateException ex, HttpServletRequest request) {
        String errorTitle = getMessage("error.title.validation.failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(), 
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleEmptyCartException(EmptyCartException ex, HttpServletRequest request) {
        String errorMessage = getMessage("error.message.borrowing.cart_is_empty");
        String errorTitle = getMessage("error.title.validation.failed");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            errorMessage,
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(),
            errorResponse,
            errorTitle
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BorrowingLimitExceededException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleBorrowingLimitExceededException(BorrowingLimitExceededException ex, HttpServletRequest request) {
        String errorTitle = getMessage("error.title.borrowing.limit_exceeded");
        
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(), 
            errorResponse, 
            errorTitle
        );
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EditionBorrowingLimitExceededException.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleEditionBorrowingLimitExceededException(EditionBorrowingLimitExceededException ex, HttpServletRequest request) {
        String errorTitle = getMessage("error.title.borrowing.limit_exceeded");
        
        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.BAD_REQUEST.value(), 
            errorResponse, 
            errorTitle
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    // xử lý lỗi khác (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseApiResponse<ErrorResponse>> handleGeneralException(Exception ex, HttpServletRequest request) {
        String errorTitle = getMessage("error.title.unexpected");

        ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );

        BaseApiResponse<ErrorResponse> apiResponse = new BaseApiResponse<>(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            errorResponse,
            errorTitle
        );

        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
