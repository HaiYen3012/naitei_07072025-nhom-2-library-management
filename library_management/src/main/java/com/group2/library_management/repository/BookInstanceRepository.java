package com.group2.library_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.group2.library_management.entity.BookInstance;
import com.group2.library_management.entity.enums.BookStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface BookInstanceRepository extends ListCrudRepository<BookInstance, Integer>,  JpaSpecificationExecutor<BookInstance> {
    Boolean existsByBarcode(String barcode);
    List<BookInstance> findByEditionIdOrderByAcquiredDateDesc(Integer editionId);

    @Modifying
    @Query("DELETE FROM BookInstance b WHERE b.id = :id")
    void permanentlyDeleteById(@Param("id") Integer id);

    List<BookInstance> findByStatus(BookStatus status);
    // Count active copies of an edition
    long countByEditionIdAndDeleteAtIsNull(Integer editionId);

    long countByEditionIdAndStatus(Integer editionId, BookStatus status);

    // Count all copies (including soft-deleted) of an edition — use native query to bypass entity-level @Where
    @Query(value = "SELECT COUNT(*) FROM book_instances WHERE edition_id = :editionId", nativeQuery = true)
    long countAllByEditionIdNative(@Param("editionId") Integer editionId);

    boolean existsByEditionIdAndDeleteAtIsNull(Integer editionId);

    // nativeQuery trả về số (0/1). Dùng Integer/Number để tránh cast lỗi.
    @Query(value = "SELECT EXISTS(SELECT 1 FROM book_instances WHERE edition_id = :editionId)", 
           nativeQuery = true)
    Integer existsByEditionIdNative(@Param("editionId") Integer editionId);

    @Query("SELECT bi FROM BookInstance bi WHERE bi.edition.id = :editionId AND bi.status = :status ORDER BY bi.acquiredDate ASC")
    List<BookInstance> findByEditionIdAndStatusOrderByAcquiredDateAsc(@Param("editionId") Integer editionId, @Param("status") BookStatus status);

    // default method thực hiện convert sang boolean
    default boolean existsByEditionId(Integer editionId) {
        Integer result = existsByEditionIdNative(editionId);
        return result != null && result != 0;
    }

}
