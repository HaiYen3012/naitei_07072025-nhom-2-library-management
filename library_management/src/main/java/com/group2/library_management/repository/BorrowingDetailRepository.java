package com.group2.library_management.repository;

import com.group2.library_management.entity.BorrowingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowingDetailRepository extends JpaRepository<BorrowingDetail, Integer> {
}
