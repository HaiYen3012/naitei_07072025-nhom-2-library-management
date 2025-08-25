package com.group2.library_management.service.impl;

import com.group2.library_management.dto.mapper.BorrowingMapper;
import com.group2.library_management.dto.request.BorrowingRequest;
import com.group2.library_management.dto.response.BorrowingRequestResponse;
import com.group2.library_management.dto.response.EditionBorrowCount;
import com.group2.library_management.entity.*;
import com.group2.library_management.entity.enums.BorrowingStatus;
import com.group2.library_management.exception.BorrowingLimitExceededException;
import com.group2.library_management.exception.EditionBorrowingLimitExceededException;
import com.group2.library_management.exception.EmptyCartException;
import com.group2.library_management.exception.InvalidPickupDateException;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.repository.BorrowingReceiptRepository;
import com.group2.library_management.repository.CartRepository;
import com.group2.library_management.service.AbstractBaseService;
import com.group2.library_management.service.BorrowingRequestService;
import com.group2.library_management.service.CartService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorrowingRequestServiceImpl extends AbstractBaseService implements BorrowingRequestService {

    private final BorrowingMapper borrowingMapper;
    private final BorrowingReceiptRepository borrowingReceiptRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final MessageSource messageSource;

    private static final int MAX_PICKUP_DELAY_DAYS = 7;
    private static final int BORROWING_DURATION_DAYS = 60;
    private static final int MAX_TOTAL_BORROWING_LIMIT = 5;
    private static final int MAX_EDITION_BORROWING_LIMIT = 2;

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    @Override
    @Transactional
    public BorrowingRequestResponse createBorrowingRequestFromCart(BorrowingRequest request) {
        // 1. Validate ngày hẹn lấy sách
        validateBorrowedDate(request.borrowedDate());

        // 2. Lấy người dùng và giỏ hàng của họ
        User currentUser = getCurrentUser();
        Cart cart = cartRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException());

        Set<CartItem> cartItems = cart.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        // 3. Validate giới hạn mượn
        validateBorrowingLimits(currentUser, cartItems);

        // 4. Chuyển đổi CartItems thành BorrowingRequestDetails
        List<BorrowingRequestDetail> requestDetails = new ArrayList<>();
        cartItems.forEach(item -> {
            requestDetails.add(BorrowingRequestDetail.builder()
                    .edition(item.getEdition())
                    .quantity(item.getQuantity())
                    .build());
        });

        // 5. Tạo BorrowingReceipt
        BorrowingReceipt receipt = BorrowingReceipt.builder()
                .user(currentUser)
                .borrowedDate(request.borrowedDate().atStartOfDay())
                .dueDate(request.borrowedDate().atStartOfDay().plusDays(BORROWING_DURATION_DAYS))
                .borrowingRequestDetails(requestDetails)
                .build();

        requestDetails.forEach(detail -> detail.setBorrowingReceipt(receipt));

        // 6. Lưu receipt
        BorrowingReceipt savedReceipt = borrowingReceiptRepository.save(receipt);

        // 7. Làm sạch giỏ hàng
        cartService.clearCart();

        return borrowingMapper.toBorrowingRequestResponse(savedReceipt);
    }

    private void validateBorrowedDate(LocalDate borrowedDate) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(MAX_PICKUP_DELAY_DAYS);

        if (borrowedDate.isAfter(maxDate)) {
            String message = getMessage(
                    "error.message.invalid_pickup_date",
                    MAX_PICKUP_DELAY_DAYS);
            throw new InvalidPickupDateException(message);
        }
    }

    /**
     * Xác thực một yêu cầu mượn sách mới dựa trên các quy tắc nghiệp vụ.
     * Phương thức này kiểm tra hai ràng buộc chính:
     * 1. Giới hạn mỗi đầu sách: Một người dùng không thể mượn quá {@code MAX_EDITION_BORROWING_LIMIT} bản cho một đầu sách duy nhất. Giới hạn này được tính trên tổng số sách trong các yêu cầu chờ duyệt và các sách đã được duyệt/đang mượn.
     * 2. Giới hạn tổng số sách mượn: Tổng số sách mà một người dùng đang mượn hoặc đang yêu cầu không được vượt quá {@code MAX_TOTAL_BORROWING_LIMIT}.
     *
     * @param user Người dùng đang thực hiện yêu cầu mượn sách.
     * @param newItems Một tập hợp các {@code CartItem}, đại diện cho các sách trong giỏ hàng của yêu cầu mượn mới.
     * @throws EditionBorrowingLimitExceededException nếu yêu cầu mới vi phạm giới hạn mượn sách cho một đầu sách cụ thể.
     * @throws BorrowingLimitExceededException nếu yêu cầu mới vi phạm tổng giới hạn số sách được phép mượn cùng một lúc.
     */
    private void validateBorrowingLimits(User user, Set<CartItem> newItems) {
        Map<Integer, Integer> editionQuantities = new HashMap<>();

        // 1: Tổng hợp số lượng sách từ các yêu cầu đang ở trạng thái chờ duyệt (PENDING).
        // Nguồn dữ liệu: bảng borrowing_request_details, tính tổng cột `quantity`.
        List<BorrowingStatus> requestStatuses = List.of(BorrowingStatus.PENDING);
        List<EditionBorrowCount> requestedCounts = borrowingReceiptRepository.findRequestedEditionCounts(user.getId(), requestStatuses);
        
        requestedCounts.forEach(dto -> 
            editionQuantities.merge(dto.editionId(), dto.count().intValue(), Integer::sum)
        );
        
        // 2: Tổng hợp số lượng sách từ các phiếu mượn đã được gán sách vật lý cụ thể.
        // Nguồn dữ liệu: bảng borrowing_details, đếm số lượng sách vật lý (book_instance). 
        List<BorrowingStatus> assignedStatuses = List.of(
                BorrowingStatus.APPROVED,
                BorrowingStatus.BORROWED,
                BorrowingStatus.OVERDUE);

        List<EditionBorrowCount> assignedCounts = borrowingReceiptRepository.findAssignedEditionCounts(user.getId(), assignedStatuses);
        
        assignedCounts.forEach(dto -> 
            editionQuantities.merge(dto.editionId(), dto.count().intValue(), Integer::sum)
        );

        // 3: Kiểm tra giới hạn mỗi đầu sách.
        for (CartItem item : newItems) {
            int editionId = item.getEdition().getId();
            int currentQuantity = editionQuantities.getOrDefault(editionId, 0);
            int newTotalForEdition = currentQuantity + item.getQuantity();

            if (newTotalForEdition > MAX_EDITION_BORROWING_LIMIT) {
                String message = getMessage(
                        "error.message.borrowing.edition_limit_exceeded",
                        MAX_EDITION_BORROWING_LIMIT,
                        item.getEdition().getTitle());
                throw new EditionBorrowingLimitExceededException(message);
            }
            editionQuantities.put(editionId, newTotalForEdition);
        }

        // 4: Kiểm tra tổng giới hạn mượn sách
        int totalBooksAfterRequest = editionQuantities.values().stream().mapToInt(Integer::intValue).sum();
        if (totalBooksAfterRequest > MAX_TOTAL_BORROWING_LIMIT) {
            String message = getMessage(
                    "error.message.borrowing.total_limit_exceeded",
                    MAX_TOTAL_BORROWING_LIMIT);
            throw new BorrowingLimitExceededException(message);
        }
    }
}
