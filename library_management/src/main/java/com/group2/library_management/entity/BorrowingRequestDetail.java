package com.group2.library_management.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "borrowing_request_details")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BorrowingRequestDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_receipt_id", nullable = false)
    private BorrowingReceipt borrowingReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edition_id", nullable = false)
    private Edition edition;

    @Column(nullable = false)
    private int quantity;
}
