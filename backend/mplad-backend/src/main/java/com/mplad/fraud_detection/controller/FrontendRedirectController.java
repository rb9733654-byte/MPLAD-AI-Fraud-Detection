package com.mplad.fraud_detection.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

/** Sends browser page requests to the frontend server; port 8080 remains the API server. */
@RestController
public class FrontendRedirectController {
    private static final Set<String> FRONTEND_PAGES = Set.of(
            "Access.html", "Login.html", "index.html", "dashboard.html",
            "authority-auth.html", "contractor-auth.html", "contractor.html",
            "public-auth.html", "public.html"
    );

    @GetMapping({
            "/Access.html", "/Login.html", "/index.html", "/dashboard.html",
            "/authority-auth.html", "/contractor-auth.html", "/contractor.html",
            "/public-auth.html", "/public.html"
    })
    public ResponseEntity<Void> redirectFrontendPage(HttpServletRequest request) {
        String page = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
        if (!FRONTEND_PAGES.contains(page)) return ResponseEntity.notFound().build();
        URI frontendUrl = URI.create(request.getScheme() + "://" + request.getServerName() + ":5500/" + page);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, frontendUrl.toString()).build();
    }
}
