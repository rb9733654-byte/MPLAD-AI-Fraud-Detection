package com.mplad.fraud_detection.service;

import java.time.Duration;

import com.mplad.fraud_detection.dto.AiPredictionRequest;
import com.mplad.fraud_detection.dto.AiPredictionResponse;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** HTTP boundary for the local synthetic anomaly-review service. */
@Component
public class PythonAnomalyClient {

    private static final String AI_SERVICE_BASE_URL = "http://127.0.0.1:8000";

    private final RestClient restClient;

    public PythonAnomalyClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(AI_SERVICE_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public AiPredictionResponse predict(AiPredictionRequest request) {
        try {
            AiPredictionResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiPredictionResponse.class);
            if (response == null) {
                throw new AiServiceUnavailableException();
            }
            return response;
        } catch (RestClientException exception) {
            throw new AiServiceUnavailableException();
        }
    }

    public static class AiServiceUnavailableException extends RuntimeException {
        public AiServiceUnavailableException() {
            super("The anomaly service is currently unavailable.");
        }
    }
}
