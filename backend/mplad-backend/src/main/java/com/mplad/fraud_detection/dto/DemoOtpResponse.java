package com.mplad.fraud_detection.dto;

/** Demo-only OTP response for the local prototype, which has no email delivery configured. */
public record DemoOtpResponse(String message, String demoCode) { }
