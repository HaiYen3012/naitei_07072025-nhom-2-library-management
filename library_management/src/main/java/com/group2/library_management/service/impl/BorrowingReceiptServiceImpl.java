package com.group2.library_management.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.group2.library_management.dto.request.UpdateBorrowingDetailRequest;
import com.group2.library_management.dto.response.BorrowingReceiptResponse;
import com.group2.library_management.entity.BookInstance;
import com.group2.library_management.entity.BorrowingDetail;
import com.group2.library_management.entity.BorrowingReceipt;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.User;
import com.group2.library_management.entity.enums.BookStatus;
import com.group2.library_management.entity.enums.BorrowingStatus;
import com.group2.library_management.repository.BookInstanceRepository;
import com.group2.library_management.repository.BorrowingDetailRepository;
import com.group2.library_management.repository.BorrowingReceiptRepository;
import com.group2.library_management.repository.EditionRepository;
import com.group2.library_management.service.BorrowingReceiptService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorrowingReceiptServiceImpl implements BorrowingReceiptService {

    private final BorrowingReceiptRepository borrowingReceiptRepository;
    private final BorrowingDetailRepository borrowingDetailRepository;
    private final BookInstanceRepository bookInstanceRepository;
    private final EditionRepository editionRepository;

    @Override
    public Page<BorrowingReceiptResponse> getAllBorrowingRequests(String keyword,
            BorrowingStatus status, Pageable pageable) {
        Specification<BorrowingReceipt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("user");
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(keyword)) {
                Join<BorrowingReceipt, User> userJoin = root.join("user");
                String pattern = "%" + keyword.toLowerCase().trim() + "%";
                predicates.add(cb.or(cb.like(cb.lower(userJoin.get("name")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BorrowingReceipt> receiptPage = borrowingReceiptRepository.findAll(spec, pageable);
        return receiptPage.map(BorrowingReceiptResponse::fromEntity);
    }

    @Override
    public BorrowingReceiptResponse getBorrowingRequestById(Integer id) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Không tìm thấy phiếu mượn với ID:" + " " + id));

        return BorrowingReceiptResponse.fromEntity(receipt, true);
    }

    @Override
    @Transactional
    public void approveBorrowingRequest(Integer id) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Không tìm thấy phiếu mượn với ID: " + id));

        // Check current status
        if (receipt.getStatus() != BorrowingStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ có thể phê duyệt yêu cầu đang ở trạng thái chờ phê duyệt");
        }

        // Validate all book instances are available before approving
        List<BookInstance> unavailableBooks = new ArrayList<>();
        receipt.getBorrowingDetails().forEach(detail -> {
            BookInstance bookInstance = detail.getBookInstance();
            if (bookInstance.getStatus() != BookStatus.AVAILABLE) {
                unavailableBooks.add(bookInstance);
            }
        });

        // If any book is not available, reject the approval
        if (!unavailableBooks.isEmpty()) {
            StringBuilder errorMessage =
                    new StringBuilder("Không thể phê duyệt: Các sách sau không còn sẵn sàng: ");
            unavailableBooks.forEach(bookInstance -> errorMessage.append(bookInstance.getEdition().getTitle())
                    .append(" (").append(bookInstance.getStatus()).append("), "));
            errorMessage.setLength(errorMessage.length() - 2); // Remove last ", "
            throw new IllegalStateException(errorMessage.toString());
        }

        // All books are available, proceed with approval
        // Update borrowing receipt status
        receipt.setStatus(BorrowingStatus.APPROVED);

        // Update status of all book instances and decrease availableQuantity
        receipt.getBorrowingDetails().forEach(detail -> {
            BookInstance bookInstance = detail.getBookInstance();
            Edition edition = bookInstance.getEdition();
            
            // Update book instance status: AVAILABLE → RESERVED (when approve request)
            bookInstance.setStatus(BookStatus.RESERVED);
            bookInstanceRepository.save(bookInstance);
            
            // Decrease availableQuantity when changing from AVAILABLE to RESERVED
            if (edition.getAvailableQuantity() > 0) {
                edition.setAvailableQuantity(edition.getAvailableQuantity() - 1);
                editionRepository.save(edition); // Save edition to update availableQuantity
            }
        });

        borrowingReceiptRepository.save(receipt);

        // TODO: Send email notification to user
        // emailService.sendApprovalNotification(receipt.getUser().getEmail(), receipt);
    }

    @Override
    @Transactional
    public void rejectBorrowingRequest(Integer id, String rejectedReason) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Không tìm thấy phiếu mượn với ID: " + id));

        // Check current status
        if (receipt.getStatus() != BorrowingStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ có thể từ chối yêu cầu đang ở trạng thái chờ phê duyệt");
        }

        // Update borrowing receipt status and rejection reason
        receipt.setStatus(BorrowingStatus.REJECTED);
        receipt.setRejectedReason(rejectedReason);

        // Save changes
        borrowingReceiptRepository.save(receipt);

        // TODO: Send email notification to user
        // emailService.sendRejectionNotification(receipt.getUser().getEmail(), receipt);
    }

    @Override
    @Transactional
    public void returnBook(Integer id) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Không tìm thấy phiếu mượn với ID: " + id));

        // Check current status - must be BORROWED to return
        if (receipt.getStatus() != BorrowingStatus.BORROWED) {
            throw new IllegalStateException(
                    "Chỉ có thể trả sách khi phiếu mượn đang ở trạng thái đã mượn");
        }

        // Update status of all book instances and increase availableQuantity
        receipt.getBorrowingDetails().forEach(detail -> {
            BookInstance bookInstance = detail.getBookInstance();
            Edition edition = bookInstance.getEdition();
            
            // Update book instance status: BORROWED → AVAILABLE (when return book)
            bookInstance.setStatus(BookStatus.AVAILABLE);
            bookInstanceRepository.save(bookInstance);
            
            // Increase availableQuantity when changing from BORROWED to AVAILABLE
            edition.setAvailableQuantity(edition.getAvailableQuantity() + 1);
            editionRepository.save(edition); // Save edition to update availableQuantity
            
            // Set return date
            detail.setRefundDate(LocalDateTime.now());
        });

        // Update borrowing receipt status to RETURNED
        receipt.setStatus(BorrowingStatus.RETURNED);
        borrowingReceiptRepository.save(receipt);

        // TODO: Send email notification to user
        // emailService.sendReturnConfirmationNotification(receipt.getUser().getEmail(), receipt);
    }

    @Override
    @Transactional
    public void updateBorrowingDetails(Integer borrowingReceiptId, List<UpdateBorrowingDetailRequest> updates) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(borrowingReceiptId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiếu mượn với ID: " + borrowingReceiptId));

        // Check if receipt status allows updating
        if (receipt.getStatus() != BorrowingStatus.APPROVED && receipt.getStatus() != BorrowingStatus.BORROWED) {
            throw new IllegalStateException("Chỉ có thể cập nhật chi tiết khi phiếu mượn ở trạng thái APPROVED hoặc BORROWED");
        }

        boolean hasChanges = false;

        for (UpdateBorrowingDetailRequest updateRequest : updates) {
            BorrowingDetail detail = borrowingDetailRepository.findById(updateRequest.getBorrowingDetailId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chi tiết đơn mượn với ID: " + updateRequest.getBorrowingDetailId()));

            // Validate that the detail belongs to this receipt
            if (!detail.getBorrowingReceipt().getId().equals(borrowingReceiptId)) {
                throw new IllegalArgumentException("Chi tiết đơn mượn không thuộc về phiếu mượn này");
            }

            switch (updateRequest.getAction()) {
                case "EXTEND":
                    handleExtendAction(detail, updateRequest);
                    hasChanges = true;
                    break;
                case "RETURN":
                    handleReturnAction(detail, updateRequest);
                    hasChanges = true;
                    break;
                case "BORROWED":
                    handleBorrowedAction(detail, updateRequest);
                    hasChanges = true;
                    break;
                case "NOT_BORROWED":
                    handleNotBorrowedAction(detail, updateRequest);
                    hasChanges = true;
                    break;
                default:
                    throw new IllegalArgumentException("Hành động không hợp lệ: " + updateRequest.getAction());
            }
        }

        if (hasChanges) {
            // Update receipt status based on current state of all details
            updateReceiptStatus(receipt);
            borrowingReceiptRepository.save(receipt);
        }
    }

    private void handleExtendAction(BorrowingDetail detail, UpdateBorrowingDetailRequest request) {
        if (request.getNewDueDate() == null) {
            throw new IllegalArgumentException("Ngày hẹn trả mới là bắt buộc khi gia hạn");
        }
        if (request.getNewDueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ngày gia hạn phải sau ngày hiện tại");
        }

        // Update the borrowing receipt's due date instead of detail's due date
        BorrowingReceipt receipt = detail.getBorrowingReceipt();
        receipt.setDueDate(request.getNewDueDate());
        borrowingReceiptRepository.save(receipt);
        
        borrowingDetailRepository.save(detail);
    }

    private void handleReturnAction(BorrowingDetail detail, UpdateBorrowingDetailRequest request) {
        LocalDateTime returnDate = request.getActualReturnDate() != null ? 
                request.getActualReturnDate() : LocalDateTime.now();
        
        if (returnDate.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ngày trả thực tế không được ở tương lai");
        }

        // Set actual return date (using refund_date field)
        detail.setActualReturnDate(returnDate);

        // Update book instance status to AVAILABLE
        BookInstance bookInstance = detail.getBookInstance();
        bookInstance.setStatus(BookStatus.AVAILABLE);
        bookInstanceRepository.save(bookInstance);

        // Increase available quantity
        Edition edition = bookInstance.getEdition();
        edition.setAvailableQuantity(edition.getAvailableQuantity() + 1);
        editionRepository.save(edition);

        borrowingDetailRepository.save(detail);
    }



    private void updateReceiptStatus(BorrowingReceipt receipt) {
        List<BorrowingDetail> details = receipt.getBorrowingDetails();
        
        if (receipt.getStatus() == BorrowingStatus.APPROVED) {
            // Logic for APPROVED → BORROWED or CANCELLED
            boolean hasBorrowed = details.stream()
                    .anyMatch(detail -> detail.getBookInstance().getStatus() == BookStatus.BORROWED);
            
            if (hasBorrowed) {
                // At least one book was borrowed → change to BORROWED
                receipt.setStatus(BorrowingStatus.BORROWED);
            } else {
                // Check if all books are available (not borrowed) → change to CANCELLED
                boolean allAvailable = details.stream()
                        .allMatch(detail -> detail.getBookInstance().getStatus() == BookStatus.AVAILABLE);
                if (allAvailable) {
                    receipt.setStatus(BorrowingStatus.CANCELLED);
                }
            }
        } else if (receipt.getStatus() == BorrowingStatus.BORROWED) {
            // Original logic for BORROWED status
            boolean hasReturned = false;
            boolean hasBorrowed = false;
            boolean hasLostOrDamaged = false;

            for (BorrowingDetail detail : details) {
                BookInstance bookInstance = detail.getBookInstance();
                
                if (detail.getActualReturnDate() != null) {
                    hasReturned = true;
                } else if (bookInstance.getStatus() == BookStatus.BORROWED) {
                    hasBorrowed = true;
                } else if (bookInstance.getStatus() == BookStatus.LOST || bookInstance.getStatus() == BookStatus.DAMAGED) {
                    hasLostOrDamaged = true;
                }
            }

            if (!hasBorrowed) {
                // No books in BORROWED status
                if (hasReturned && hasLostOrDamaged) {
                    receipt.setStatus(BorrowingStatus.LOST_REPORTED);
                } else if (hasReturned && !hasLostOrDamaged) {
                    receipt.setStatus(BorrowingStatus.RETURNED);
                } else if (!hasReturned && hasLostOrDamaged) {
                    receipt.setStatus(BorrowingStatus.LOST_REPORTED);
                }
            } else {
                // Still has books in BORROWED status
                receipt.setStatus(BorrowingStatus.BORROWED);
                
                // Check if overdue
                LocalDateTime now = LocalDateTime.now();
                boolean isOverdue = receipt.getDueDate() != null && receipt.getDueDate().isBefore(now);
                
                if (isOverdue) {
                    receipt.setStatus(BorrowingStatus.OVERDUE);
                }
            }
        }
    }

    private void handleBorrowedAction(BorrowingDetail detail, UpdateBorrowingDetailRequest request) {
        // Mark book as borrowed - don't change book instance status as it's already borrowed
        // This is used when confirming pickup from APPROVED status
        borrowingDetailRepository.save(detail);
    }

    private void handleNotBorrowedAction(BorrowingDetail detail, UpdateBorrowingDetailRequest request) {
        // Mark book as not borrowed - return book to available status
        BookInstance bookInstance = detail.getBookInstance();
        bookInstance.setStatus(BookStatus.AVAILABLE);
        bookInstanceRepository.save(bookInstance);

        // Increase available quantity
        Edition edition = bookInstance.getEdition();
        edition.setAvailableQuantity(edition.getAvailableQuantity() + 1);
        editionRepository.save(edition);

        borrowingDetailRepository.save(detail);
    }
}
