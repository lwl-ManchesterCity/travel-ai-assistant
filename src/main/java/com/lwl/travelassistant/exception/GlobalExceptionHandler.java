package com.lwl.travelassistant.exception;

import com.lwl.travelassistant.model.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> details = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        log.warn("请求参数校验失败: {}", details);

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "VALIDATION_ERROR",
                "请求参数校验失败",
                details,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(TripPlanningException.class)
    public ResponseEntity<ApiErrorResponse> handleTripPlanningException(TripPlanningException exception) {
        log.warn("旅行规划异常: {}", exception.getMessage(), exception);
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "TRIP_PLANNING_ERROR",
                exception.getMessage(),
                List.of("请调整输入条件后重试"),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception) {
        log.error("服务内部异常", exception);
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "INTERNAL_ERROR",
                "服务内部出现异常",
                List.of(exception.getClass().getSimpleName()),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
