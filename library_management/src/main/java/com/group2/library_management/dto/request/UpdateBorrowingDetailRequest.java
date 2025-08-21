package com.group2.library_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBorrowingDetailRequest {
    @NotNull
    private Integer borrowingDetailId;
    
    @NotNull
    private String action; // "EXTEND", "RETURN", "LOST", "DAMAGED"
    
    private LocalDateTime newDueDate; // For extend action
    
    private LocalDateTime actualReturnDate; // For return action
    
    private String notes; // For lost/damaged actions
}
