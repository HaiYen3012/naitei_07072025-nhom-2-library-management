package com.group2.library_management.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice(basePackages = "com.group2.library_management.controller.admin")
public class GlobalExceptionHandler {

    /**
     * Catches and handles all EntityNotFoundException exceptions
     * thrown from web controllers (admin package only).
     * @param ex The caught exception.
     * @param model Model to pass error message to the view.
     * @return Name of the error template to be rendered.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEntityNotFound(EntityNotFoundException ex, Model model) {
        log.warn("Resource not found requested in web controller: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404"; // Points to view error/404.html for web controllers
    }
}
