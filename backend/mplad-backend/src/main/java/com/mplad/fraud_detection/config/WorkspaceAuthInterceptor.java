package com.mplad.fraud_detection.config;

import com.mplad.fraud_detection.service.WorkspaceAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WorkspaceAuthInterceptor implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
                String uri = request.getRequestURI();
                String base = request.getContextPath() + "/api/";
                String role = uri.startsWith(base + "contractors/")
                        || (uri.endsWith("/progress-evidence") && "POST".equalsIgnoreCase(request.getMethod()))
                        ? "CONTRACTOR" : "AUTHORITY";
                HttpSession session = request.getSession(false);
                if (session == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Sign in to continue.");
                WorkspaceAuthService.requireRole(session, role);
                if ("POST".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getMethod())
                        || "PUT".equalsIgnoreCase(request.getMethod()) || "DELETE".equalsIgnoreCase(request.getMethod()))
                    WorkspaceAuthService.requireCsrf(session, request.getHeader("X-CSRF-Token"));
                if ("CONTRACTOR".equals(role) && uri.startsWith(base + "contractors/")) {
                    String pathId = uri.substring((base + "contractors/").length()).split("/", 2)[0];
                    if (!pathId.equals(session.getAttribute(WorkspaceAuthService.IDENTITY)))
                        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "This contractor account cannot access another contractor's records.");
                }
                return true;
            }
        }).addPathPatterns("/api/projects/**", "/api/fund-utilization/**", "/api/contractors/**");
    }
}
