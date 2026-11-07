package com.gapmap.controller;

import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ExceptionHandlerAdvice {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "message",
                "Please check the form",
                "errors",
                e.getBindingResult().getFieldErrors().stream()
                    .map(x -> x.getField() + ": " + x.getDefaultMessage())
                    .toList()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(Map.of("message", Objects.toString(e.getReason(), "Request failed")));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> denied(Exception e) {
    return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler({
    org.springframework.dao.DataIntegrityViolationException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class
  })
  ResponseEntity<?> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("message", "This record is already used or changed. Refresh and try again."));
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> malformed(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("message", "Invalid request format"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed", e);
    return ResponseEntity.status(500)
        .body(Map.of("message", "An unexpected error occurred. Please try again."));
  }
}
