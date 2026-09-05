package com.mplad.fraud_detection.dto;

/** Safe, client-facing error message without implementation details. */
public record ApiErrorResponse(String message) {
}
