package com.collab.workspace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

// 🔴 Handle custom ApiError / RuntimeException
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<?> handleRuntime(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            Map.of(
                    "error", ex.getMessage()
            )
    );
}

// 🔴 Handle Spring ResponseStatusException
@ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
public ResponseEntity<?> handleResponseStatus(org.springframework.web.server.ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(
            Map.of(
                    "error", ex.getReason()
            )
    );
}

// 🔴 Handle all other unexpected errors
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleGeneral(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            Map.of(
                    "error", "Something went wrong",
                    "details", ex.getMessage()
            )
    );
}
}
