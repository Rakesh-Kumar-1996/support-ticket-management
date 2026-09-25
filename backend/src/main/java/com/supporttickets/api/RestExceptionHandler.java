package com.supporttickets.api;

import com.supporttickets.domain.InvalidStatusTransitionException;
import com.supporttickets.domain.TicketNotFoundException;
import com.supporttickets.domain.TicketValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class RestExceptionHandler {

    static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    static final String TICKET_NOT_FOUND = "TICKET_NOT_FOUND";
    static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(TicketValidationException.class)
    public ResponseEntity<ApiError> handleValidation(TicketValidationException ex, HttpServletRequest request) {
        List<FieldErrorItem> fieldErrors = ex.getField() == null
                ? List.of()
                : List.of(new FieldErrorItem(ex.getField(), ex.getMessage()));
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), path(request), fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldErrorItem(err.getField(), err.getDefaultMessage()))
                .toList();
        String message = fieldErrors.isEmpty() ? "Request is invalid." : fieldErrors.getFirst().message();
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, message, path(request), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("Malformed request body");
        return error(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR,
                "Malformed JSON or invalid request body.",
                path(request),
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = UUID.class.equals(ex.getRequiredType())
                ? "Ticket id must be a UUID."
                : "Request parameter is invalid.";
        String field = ex.getName();
        return error(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR,
                message,
                path(request),
                field == null ? List.of() : List.of(new FieldErrorItem(field, message))
        );
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TicketNotFoundException ex, HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                TICKET_NOT_FOUND,
                "Ticket was not found.",
                path(request),
                List.of()
        );
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleConflict(InvalidStatusTransitionException ex, HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                INVALID_STATUS_TRANSITION,
                ex.getMessage(),
                path(request),
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error");
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                path(request),
                List.of()
        );
    }

    private static ResponseEntity<ApiError> error(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<FieldErrorItem> fieldErrors
    ) {
        ApiError body = new ApiError(
                status.value(),
                status.name(),
                code,
                message,
                path,
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
