package com.group2.library_management.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BorrowingRequest(
    @NotNull(message = "{validation.borrowedDate.not_null}")
    @Future(message = "{validation.borrowedDate.future}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    LocalDate borrowedDate 
) {}
