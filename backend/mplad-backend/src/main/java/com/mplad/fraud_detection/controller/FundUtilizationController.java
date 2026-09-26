package com.mplad.fraud_detection.controller;

import com.mplad.fraud_detection.dto.FundUtilizationOverviewResponse;
import com.mplad.fraud_detection.service.FundUtilizationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Portfolio-level fund-utilization monitoring APIs. */
@RestController
@RequestMapping("/api/fund-utilization")
@CrossOrigin(origins = {
        "http://localhost:3000", "http://localhost:5173", "http://localhost:5500", "http://localhost:5502", "http://localhost:8000",
        "http://127.0.0.1:5500", "http://127.0.0.1:5502", "http://127.0.0.1:8000"
}, allowCredentials = "true")
public class FundUtilizationController {

    private final FundUtilizationService fundUtilizationService;

    public FundUtilizationController(FundUtilizationService fundUtilizationService) {
        this.fundUtilizationService = fundUtilizationService;
    }

    @GetMapping("/overview")
    public FundUtilizationOverviewResponse getOverview() {
        return fundUtilizationService.getOverview();
    }
}
