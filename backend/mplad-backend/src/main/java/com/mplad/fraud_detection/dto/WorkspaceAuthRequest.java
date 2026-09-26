package com.mplad.fraud_detection.dto;
public record WorkspaceAuthRequest(String identity, String username, String password, String confirmPassword) { }
