package com.transfer.application.system.handlers;

import com.transfer.application.dtos.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ex.getReason())
                .status(ex.getStatusCode().value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {

        List<ErrorResponse.Field> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> {
                    return ErrorResponse.Field.builder()
                            .field(err.getField())
                            .message(err.getDefaultMessage())
                            .build();
                })
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .fields(fields)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
