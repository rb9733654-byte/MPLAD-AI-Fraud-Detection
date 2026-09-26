package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.ApiErrorResponse;
import com.mplad.fraud_detection.service.ProjectAnalysisService.ProjectNotFoundException;
import com.mplad.fraud_detection.service.ProjectHistoryService.ProjectHistoryNotFoundException;
import com.mplad.fraud_detection.service.FundUtilizationService.FundUtilizationProjectNotFoundException;
import com.mplad.fraud_detection.service.PythonAnomalyClient.AiServiceUnavailableException;
import com.mplad.fraud_detection.service.PublicAuthService.InvalidOtpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;

/** Maps expected analysis errors to safe API responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOtp(InvalidOtpException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse("That account or record already exists."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseFailure(DataAccessException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("Database operation failed. Verify the MySQL connection and required database migrations."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleServiceUnavailable(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("The requested prototype service is not enabled or is temporarily unavailable."));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("Access to this resource is not permitted."));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("Project not found."));
    }

    @ExceptionHandler(ProjectHistoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProjectHistoryNotFound(ProjectHistoryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("Project not found."));
    }

    @ExceptionHandler(FundUtilizationProjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleFundUtilizationProjectNotFound(
            FundUtilizationProjectNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("Project not found."));
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleAiServiceUnavailable(AiServiceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("Anomaly service is unavailable. Please try again later."));
    }
}
