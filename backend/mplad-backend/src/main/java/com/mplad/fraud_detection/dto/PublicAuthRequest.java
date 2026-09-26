package com.mplad.fraud_detection.dto;
public record PublicAuthRequest(String email, String password, String confirmPassword, String otp) { }
