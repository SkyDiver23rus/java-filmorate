package ru.yandex.practicum.filmorate.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 для валидации
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404 для ненайденных объектов
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation error"));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> handleEmptyResult(EmptyResultDataAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Resource not found"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex) {
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Database error: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAnyException(Exception ex) {
        ex.printStackTrace(); // Для отладки
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
    }
}