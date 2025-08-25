package com.group2.library_management.repository;

import java.util.List;
import java.util.Optional;
import com.group2.library_management.dto.response.EditionBorrowCount;
import com.group2.library_management.entity.BorrowingReceipt;
import com.group2.library_management.entity.enums.BorrowingStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowingReceiptRepository extends JpaRepository<BorrowingReceipt, Integer>,
        JpaSpecificationExecutor<BorrowingReceipt> {

    @EntityGraph(attributePaths = {
            "user",
            "borrowingDetails.bookInstance.edition"
    })
    Optional<BorrowingReceipt> findById(Integer id);

    @EntityGraph(attributePaths = {
            "borrowingDetails.bookInstance"
    })
    List<BorrowingReceipt> findByStatus(BorrowingStatus status);

    @Query("""
            SELECT br
            FROM BorrowingReceipt br
            LEFT JOIN FETCH br.borrowingDetails bd
            LEFT JOIN FETCH bd.bookInstance bi
            WHERE br.status = :status AND br.dueDate < CURRENT_TIMESTAMP
            """)
    List<BorrowingReceipt> findOverdueReceiptsByStatus(@Param("status") BorrowingStatus status);

    /**
     * Lấy tổng số lượng sách đang mượn trong các phiếu mượn đã được duyệt và đang
     * trong quá trình mượn.
     * Query này đếm các bản ghi trong borrowing_details.
     * 
     * @param userId           ID của người dùng
     * @param assignedStatuses Danh sách các trạng thái như APPROVED, BORROWED,
     *                         OVERDUE
     * @return Danh sách DTO chứa số lượng sách đang mượn cho mỗi edition.
     */
    @Query("""
            SELECT new com.group2.library_management.dto.response.EditionBorrowCount(bi.edition.id, COUNT(bi))
            FROM BorrowingReceipt br
            JOIN br.borrowingDetails bd
            JOIN bd.bookInstance bi
            WHERE br.user.id = :userId AND br.status IN :assignedStatuses
            GROUP BY bi.edition.id
            """)
    List<EditionBorrowCount> findAssignedEditionCounts(
            @Param("userId") Integer userId,
            @Param("assignedStatuses") List<BorrowingStatus> assignedStatuses);

    /**
     * Lấy tổng số lượng sách trong các yêu cầu đang chờ duyệt (PENDING).
     * Query này tính tổng số lượng từ borrowing_request_details.
     * 
     * @param userId          ID của người dùng
     * @param requestStatuses Danh sách các trạng thái
     * @return Danh sách DTO chứa số lượng sách đang yêu cầu cho mỗi edition.
     */
    @Query("""
            SELECT new com.group2.library_management.dto.response.EditionBorrowCount(brd.edition.id, SUM(brd.quantity))
            FROM BorrowingReceipt br
            JOIN br.borrowingRequestDetails brd
            WHERE br.user.id = :userId AND br.status IN :requestStatuses
            GROUP BY brd.edition.id
            """)
    List<EditionBorrowCount> findRequestedEditionCounts(
            @Param("userId") Integer userId,
            @Param("requestStatuses") List<BorrowingStatus> requestStatuses);
}
