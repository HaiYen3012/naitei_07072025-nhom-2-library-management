package com.group2.library_management.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group2.library_management.entity.BorrowingRequestDetail;

@Repository
public interface BorrowingRequestDetailRepository extends JpaRepository<BorrowingRequestDetail, Integer> {
    List<BorrowingRequestDetail> findByBorrowingReceiptId(Integer borrowingReceiptId);
}
